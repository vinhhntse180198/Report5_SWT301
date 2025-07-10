# Test Cases JUnit5 cho 3 Chức Năng Yêu Cầu Đăng Nhập

## Tổng Quan
Dự án này bao gồm 3 chức năng chính yêu cầu xác thực (authentication) với tổng cộng 15 test case được viết bằng JUnit5.

## 1. Authentication (Đăng Nhập) - 5 Test Cases

### File: `AuthenticationControllerTest.java`

#### Test Cases:
1. **testLoginSuccess()** - Đăng nhập thành công với tài khoản hợp lệ
   - Input: Username và password đúng
   - Expected: HTTP 200 OK với JWT token
   - Mock: AuthenticationManager, UserDetailsService, JwtUtil

2. **testLoginWrongPassword()** - Đăng nhập thất bại với mật khẩu sai
   - Input: Username đúng, password sai
   - Expected: HTTP 500 Internal Server Error
   - Mock: AuthenticationManager throw BadCredentialsException

3. **testLoginUserNotFound()** - Đăng nhập thất bại với tài khoản không tồn tại
   - Input: Username không tồn tại
   - Expected: HTTP 500 Internal Server Error
   - Mock: AuthenticationManager throw BadCredentialsException

4. **testLoginEmptyCredentials()** - Đăng nhập thất bại với thông tin rỗng
   - Input: Username và password rỗng
   - Expected: HTTP 500 Internal Server Error

5. **testLoginNullCredentials()** - Đăng nhập thất bại với thông tin null
   - Input: Username và password null
   - Expected: HTTP 500 Internal Server Error

## 2. Appointment Management (Quản Lý Lịch Hẹn) - 5 Test Cases

### File: `AppointmentControllerTest.java`

#### Test Cases:
1. **testCreateAppointmentSuccess()** - Tạo lịch hẹn thành công khi đã đăng nhập
   - Input: AppointmentRequest với dữ liệu hợp lệ
   - Expected: HTTP 200 OK
   - Mock: Authentication, AppointmentService

2. **testCreateAppointmentWithoutAuthentication()** - Tạo lịch hẹn thất bại khi chưa đăng nhập
   - Input: AppointmentRequest không có authentication
   - Expected: HTTP 403 Forbidden
   - Mock: SecurityContext với authentication null

3. **testCreateAppointmentWithInvalidData()** - Tạo lịch hẹn thất bại với dữ liệu không hợp lệ
   - Input: AppointmentRequest với dữ liệu thiếu/sai
   - Expected: HTTP 400 Bad Request
   - Mock: Authentication

4. **testViewUserAppointmentsSuccess()** - Xem danh sách lịch hẹn thành công
   - Input: User đã đăng nhập
   - Expected: HTTP 200 OK
   - Mock: Authentication, AppointmentService

5. **testViewUserAppointmentsWithoutAuthentication()** - Xem danh sách lịch hẹn thất bại khi không đăng nhập
   - Input: Không có authentication
   - Expected: HTTP 403 Forbidden
   - Mock: SecurityContext với authentication null

## 3. Feedback Management (Quản Lý Phản Hồi) - 5 Test Cases

### File: `FeedbackControllerTest.java`

#### Test Cases:
1. **testCreateFeedbackSuccess()** - Tạo phản hồi thành công khi đã đăng nhập
   - Input: FeedbackRequest với rating và content hợp lệ
   - Expected: HTTP 200 OK
   - Mock: Authentication, FeedbackService

2. **testCreateFeedbackWithoutAuthentication()** - Tạo phản hồi thất bại khi chưa đăng nhập
   - Input: FeedbackRequest không có authentication
   - Expected: HTTP 403 Forbidden
   - Mock: SecurityContext với authentication null

3. **testCreateFeedbackWithEmptyComment()** - Tạo phản hồi thất bại với nội dung rỗng
   - Input: FeedbackRequest với content rỗng
   - Expected: HTTP 400 Bad Request
   - Mock: Authentication

4. **testSearchFeedbacksByServiceNameSuccess()** - Tìm kiếm phản hồi theo tên dịch vụ thành công
   - Input: Service name và keyword
   - Expected: HTTP 200 OK
   - Mock: FeedbackService

5. **testUpdateFeedbackSuccess()** - Cập nhật phản hồi thành công
   - Input: FeedbackRequest với dữ liệu mới
   - Expected: HTTP 200 OK
   - Mock: Authentication, FeedbackService

## Cấu Trúc Test

### Dependencies cần thiết:
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Cấu trúc chung của mỗi test class:
- Sử dụng `@ExtendWith(MockitoExtension.class)`
- Mock các dependencies cần thiết
- Setup MockMvc và SecurityContext
- Mỗi test method có 3 phần: Arrange, Act, Assert

### Chạy Test:
```bash
# Chạy tất cả test
mvn test

# Chạy test cụ thể
mvn test -Dtest=AuthenticationControllerTest
mvn test -Dtest=AppointmentControllerTest
mvn test -Dtest=FeedbackControllerTest
```

## Lưu Ý
- Tất cả test cases đều sử dụng MockMvc để test REST endpoints
- Security context được mock để test authentication
- Các service layer được mock để isolate controller testing
- Validation errors được test với HTTP 400 Bad Request
- Authentication errors được test với HTTP 403 Forbidden 