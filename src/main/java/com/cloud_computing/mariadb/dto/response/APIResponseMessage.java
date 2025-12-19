package com.cloud_computing.mariadb.dto.response;

public enum APIResponseMessage {
    SUCCESSFULLY_CREATED("Tạo thành công."),
    SUCCESSFULLY_UPDATED("Cập nhật thành công."),
    SUCCESSFULLY_DELETED("Xóa thành công."),
    SUCCESSFULLY_RETRIEVED("Lấy dữ liệu thành công."),
    SUCCESSFULLY_LOGIN("Đăng nhập thành công."),
    SUCCESSFULLY_REGISTER("Đăng ký thành công."),
    SUCCESSFULLY_LOGOUT("Đăng xuất thành công.");

    final String message;
    APIResponseMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

