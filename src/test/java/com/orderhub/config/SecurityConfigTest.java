package com.orderhub.config;

import com.orderhub.controller.AuthController;
import com.orderhub.controller.AdminUserController;
import com.orderhub.controller.ProductController;
import com.orderhub.dto.response.ProductResponse;
import com.orderhub.entity.User;
import com.orderhub.entity.enums.Role;
import com.orderhub.repository.UserRepository;
import com.orderhub.security.JwtTokenProvider;
import com.orderhub.service.ProductService;
import com.orderhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, ProductController.class, AdminUserController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ProductService productService;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void loginApiIsPublic() throws Exception {
        User user = User.builder()
                .id(7L)
                .username("staff")
                .password("encoded")
                .email("staff@orderhub.com")
                .fullName("Staff User")
                .role(Role.STAFF)
                .active(true)
                .build();
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtTokenProvider.generateToken("staff", "STAFF")).thenReturn("test-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"staff\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", is("test-token")))
                .andExpect(jsonPath("$.data.userId", is(7)))
                .andExpect(jsonPath("$.data.role", is("STAFF")));
    }

    @Test
    void staffCannotManageStaffAccounts() throws Exception {
        mockMvc.perform(get("/api/admin/staffs")
                        .with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManageStaffAccounts() throws Exception {
        when(userService.getAllStaffs()).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/staffs")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void staffCanAccessInternalOperations() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.<ProductResponse>of());

        mockMvc.perform(get("/api/products")
                        .with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
    }

    @Test
    void internalApisRequireAnEmployeeAccount() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveStaffCannotLogin() throws Exception {
        User inactive = User.builder()
                .username("former-staff")
                .password("encoded")
                .email("former@orderhub.com")
                .fullName("Former Staff")
                .role(Role.STAFF)
                .active(false)
                .build();
        when(userRepository.findByUsername("former-staff")).thenReturn(Optional.of(inactive));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"former-staff\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
