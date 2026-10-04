package com.orderhub.entity.enums;

public enum InventoryActionType {
    RESERVE,       // Giữ chỗ khi tạo đơn (tăng reserved_quantity)
    RELEASE,       // Giải phóng khi hủy đơn (giảm reserved_quantity)
    SHIP_DEDUCT,   // Xuất kho giao hàng (giảm total_quantity & reserved_quantity)
    RETURN_RESTOCK, // Khách trả hàng / Giao thất bại hoàn lại vào kho
    IMPORT,        // Nhập thêm hàng vào kho (tăng total_quantity)
    ADMIN_ADJUST   // Điều chỉnh thủ công (kiểm kê thất thoát/hư hỏng)
}