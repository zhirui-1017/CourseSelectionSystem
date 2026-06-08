package org.example.courseselectionsystem.user;

import org.example.courseselectionsystem.entity.Admin;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.entity.User;
import org.example.courseselectionsystem.common.Constants;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.mapper.StudentMapper;
import org.example.courseselectionsystem.mapper.TeacherMapper;
import org.example.courseselectionsystem.repository.AdminRepository;
import org.example.courseselectionsystem.service.impl.UserServiceImpl;
import org.example.courseselectionsystem.vo.LoginRequest;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private TeacherMapper teacherMapper;

    @Mock
    private AdminRepository adminRepository;

    @Test
    void loginAuthenticatesStudentPasswordAndReturnsUser() {
        UserServiceImpl service = newService();
        Student student = student(1L, "S1001", "Alice");
        student.setPassword(new BCryptPasswordEncoder().encode("abc123"));
        when(studentMapper.selectByStudentNo("S1001")).thenReturn(student);

        Map<String, Object> result = service.login(loginRequest("S1001", "abc123"));

        assertThat(result.get("token")).asString().startsWith("session-");
        User user = (User) result.get("user");
        assertThat(user.getUsername()).isEqualTo("S1001");
        assertThat(user.getUserType()).isEqualTo(1);
    }

    @Test
    void loginRejectsWrongStudentPassword() {
        UserServiceImpl service = newService();
        Student student = student(1L, "S1001", "Alice");
        student.setPassword("abc123");
        when(studentMapper.selectByStudentNo("S1001")).thenReturn(student);

        assertThatThrownBy(() -> service.login(loginRequest("S1001", "bad123")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(Constants.UNAUTHORIZED_CODE);
    }

    @Test
    void loginFallsThroughToTeacherWhenStudentNotFound() {
        UserServiceImpl service = newService();
        Teacher teacher = teacher(2L, "T1001", "Carol");
        teacher.setPassword("teach123");
        when(studentMapper.selectByStudentNo("T1001")).thenReturn(null);
        when(teacherMapper.selectByTeacherNo("T1001")).thenReturn(teacher);

        Map<String, Object> result = service.login(loginRequest("T1001", "teach123"));

        User user = (User) result.get("user");
        assertThat(user.getUsername()).isEqualTo("T1001");
        assertThat(user.getUserType()).isEqualTo(2);
    }

    @Test
    void loginAuthenticatesPersistedAdmin() {
        UserServiceImpl service = newService();
        Admin admin = admin(4L, "admin");
        admin.setPassword(new BCryptPasswordEncoder().encode("admin123"));
        when(studentMapper.selectByStudentNo("admin")).thenReturn(null);
        when(teacherMapper.selectByTeacherNo("admin")).thenReturn(null);
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        Map<String, Object> result = service.login(loginRequest("admin", "admin123"));

        User user = (User) result.get("user");
        assertThat(user.getId()).isEqualTo(4L);
        assertThat(user.getUserType()).isEqualTo(3);
    }

    @Test
    void loginSupportsLegacyAdminFallback() {
        UserServiceImpl service = newService();
        when(studentMapper.selectByStudentNo("admin")).thenReturn(null);
        when(teacherMapper.selectByTeacherNo("admin")).thenReturn(null);
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.empty());

        Map<String, Object> result = service.login(loginRequest("admin", "admin123"));

        User user = (User) result.get("user");
        assertThat(user.getId()).isZero();
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getUserType()).isEqualTo(3);
    }

    @Test
    void registerCreatesStudentUserWithEncodedPassword() {
        UserServiceImpl service = newService();
        RegisterRequest request = registerRequest();
        when(studentMapper.selectByStudentNo("S1001")).thenReturn(null);
        when(studentMapper.insert(any(Student.class))).thenReturn(1);

        boolean result = service.register(request);

        assertThat(result).isTrue();
        verify(studentMapper).insert(argThat(student ->
                "S1001".equals(student.getStudentNo())
                        && "New Student".equals(student.getName())
                        && "new@example.com".equals(student.getEmail())
                        && "13900000000".equals(student.getPhone())
                        && Long.valueOf(2L).equals(student.getCollegeId())
                        && Long.valueOf(3L).equals(student.getMajorId())
                        && "Class 1".equals(student.getClassName())
                        && Integer.valueOf(1).equals(student.getStatus())
                        && new BCryptPasswordEncoder().matches("abc123", student.getPassword())
        ));
    }

    @Test
    void registerRejectsDuplicateStudentNo() {
        UserServiceImpl service = newService();
        RegisterRequest request = registerRequest();
        when(studentMapper.selectByStudentNo("S1001")).thenReturn(student(1L, "S1001", "Existing"));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(Constants.DUPLICATE_CODE);
    }

    @Test
    void registerRejectsMismatchedConfirmPassword() {
        UserServiceImpl service = newService();
        RegisterRequest request = registerRequest();
        request.confirmPassword = "other123";

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(Constants.PARAM_ERROR_CODE);
    }

    @Test
    void registerAcceptsLowercaseStudentRole() {
        UserServiceImpl service = newService();
        RegisterRequest request = registerRequest();
        request.role = "student";
        when(studentMapper.selectByStudentNo("S1001")).thenReturn(null);
        when(studentMapper.insert(any(Student.class))).thenReturn(1);

        boolean result = service.register(request);

        assertThat(result).isTrue();
    }

    @Test
    void registerCreatesTeacherUserWithEncodedPassword() {
        UserServiceImpl service = newService();
        RegisterRequest request = teacherRegisterRequest();
        when(teacherMapper.selectByTeacherNo("T1001")).thenReturn(null);
        when(teacherMapper.insert(any(Teacher.class))).thenReturn(1);

        boolean result = service.register(request);

        assertThat(result).isTrue();
        verify(teacherMapper).insert(argThat(teacher ->
                "T1001".equals(teacher.getTeacherNo())
                        && "New Teacher".equals(teacher.getName())
                        && "teacher@example.com".equals(teacher.getEmail())
                        && "13800000001".equals(teacher.getPhone())
                        && Long.valueOf(5L).equals(teacher.getDepartmentId())
                        && Integer.valueOf(1).equals(teacher.getStatus())
                        && new BCryptPasswordEncoder().matches("teach123", teacher.getPassword())
        ));
    }

    @Test
    void registerRejectsDuplicateTeacherNo() {
        UserServiceImpl service = newService();
        RegisterRequest request = teacherRegisterRequest();
        when(teacherMapper.selectByTeacherNo("T1001")).thenReturn(teacher(1L, "T1001", "Existing"));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(Constants.DUPLICATE_CODE);
    }

    @Test
    void registerCreatesAdminUserWithEncodedPassword() {
        UserServiceImpl service = newService();
        RegisterRequest request = adminRegisterRequest();
        when(adminRepository.findByUsername("new-admin")).thenReturn(Optional.empty());

        boolean result = service.register(request);

        assertThat(result).isTrue();
        verify(adminRepository).save(argThat(admin ->
                "new-admin".equals(admin.getUsername())
                        && Integer.valueOf(3).equals(admin.getRole())
                        && Integer.valueOf(1).equals(admin.getStatus())
                        && new BCryptPasswordEncoder().matches("admin123", admin.getPassword())
        ));
    }

    @Test
    void registerRejectsUnsupportedUserType() {
        UserServiceImpl service = newService();
        RegisterRequest request = registerRequest();
        request.role = "guest";
        request.userType = 9;

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(Constants.PARAM_ERROR_CODE);
    }

    @Test
    void getUserListAggregatesFiltersSortsAndPagesUsers() {
        UserServiceImpl service = newService();
        when(studentMapper.selectAll()).thenReturn(List.of(
                student(2L, "S0002", "Bob"),
                student(1L, "S0001", "Alice")
        ));
        when(teacherMapper.selectAll()).thenReturn(List.of(teacher(3L, "T0001", "Carol")));
        when(adminRepository.findAll()).thenReturn(List.of(admin(4L, "admin")));
        PageRequest request = new PageRequest();
        request.setPageNum(1);
        request.setPageSize(2);
        request.setOrderByColumn("username");
        request.setIsAsc("asc");

        Page<User> page = service.getUserList(request, "S", null, null);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(User::getUsername)
                .containsExactly("S0001", "S0002");
    }

    @Test
    void getUsersByUserTypeSupportsAdminUsers() {
        UserServiceImpl service = newService();
        when(adminRepository.findAll()).thenReturn(List.of(admin(4L, "admin")));

        List<User> users = service.getUsersByUserType(3);

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUsername()).isEqualTo("admin");
        assertThat(users.get(0).getUserType()).isEqualTo(3);
    }

    @Test
    void getUserByIdSupportsLegacyAdminIdZero() {
        UserServiceImpl service = newService();

        User user = service.getUserById(0L);

        assertThat(user.getId()).isZero();
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getUserType()).isEqualTo(3);
    }

    @Test
    void getUserByIdSupportsPersistedAdminUser() {
        UserServiceImpl service = newService();
        when(studentMapper.selectById(4L)).thenReturn(null);
        when(teacherMapper.selectById(4L)).thenReturn(null);
        when(adminRepository.findById(4L)).thenReturn(Optional.of(admin(4L, "admin")));

        User user = service.getUserById(4L);

        assertThat(user.getId()).isEqualTo(4L);
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getUserType()).isEqualTo(3);
    }

    @Test
    void getUserByUsernameSupportsPersistedAdminUser() {
        UserServiceImpl service = newService();
        when(studentMapper.selectByStudentNo("admin")).thenReturn(null);
        when(teacherMapper.selectByTeacherNo("admin")).thenReturn(null);
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin(4L, "admin")));

        User user = service.getUserByUsername("admin");

        assertThat(user.getId()).isEqualTo(4L);
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getUserType()).isEqualTo(3);
    }

    @Test
    void resetPasswordUpdatesStudentPassword() {
        UserServiceImpl service = newService();
        Student student = student(2L, "S0002", "Bob");
        student.setPassword("old");
        when(studentMapper.selectById(2L)).thenReturn(student);
        when(studentMapper.updateById(any(Student.class))).thenReturn(1);

        boolean result = service.resetPassword(2L, "new123");

        assertThat(result).isTrue();
        assertThat(student.getPassword()).isEqualTo("new123");
    }

    @Test
    void changePasswordChecksTeacherOldPasswordAndSavesNewPassword() {
        UserServiceImpl service = newService();
        Teacher teacher = teacher(3L, "T0001", "Carol");
        teacher.setPassword("old123");
        when(studentMapper.selectById(3L)).thenReturn(null);
        when(teacherMapper.selectById(3L)).thenReturn(teacher);
        when(teacherMapper.updateById(any(Teacher.class))).thenReturn(1);

        boolean result = service.changePassword(3L, "old123", "new123");

        assertThat(result).isTrue();
        assertThat(teacher.getPassword()).isEqualTo("new123");
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        UserServiceImpl service = newService();
        Student student = student(2L, "S0002", "Bob");
        student.setPassword("old123");
        when(studentMapper.selectById(2L)).thenReturn(student);

        assertThatThrownBy(() -> service.changePassword(2L, "bad", "new123"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(Constants.PARAM_ERROR_CODE);
    }

    @Test
    void resetPasswordUpdatesAdminPassword() {
        UserServiceImpl service = newService();
        Admin admin = admin(4L, "admin");
        when(studentMapper.selectById(4L)).thenReturn(null);
        when(teacherMapper.selectById(4L)).thenReturn(null);
        when(adminRepository.findById(4L)).thenReturn(Optional.of(admin));

        boolean result = service.resetPassword(4L, "newAdmin123");

        assertThat(result).isTrue();
        assertThat(admin.getPassword()).isEqualTo("newAdmin123");
        verify(adminRepository).save(admin);
    }

    private UserServiceImpl newService() {
        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        ReflectionTestUtils.setField(service, "teacherMapper", teacherMapper);
        ReflectionTestUtils.setField(service, "adminRepository", adminRepository);
        return service;
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.username = "student-user";
        request.userCode = "S1001";
        request.password = "abc123";
        request.confirmPassword = "abc123";
        request.realName = "New Student";
        request.role = Constants.ROLE_STUDENT;
        request.departmentId = 2L;
        request.majorId = 3L;
        request.className = "Class 1";
        request.email = "new@example.com";
        request.phone = "13900000000";
        return request;
    }

    private RegisterRequest teacherRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.username = "teacher-user";
        request.userCode = "T1001";
        request.password = "teach123";
        request.confirmPassword = "teach123";
        request.realName = "New Teacher";
        request.role = Constants.ROLE_TEACHER;
        request.departmentId = 5L;
        request.email = "teacher@example.com";
        request.phone = "13800000001";
        return request;
    }

    private RegisterRequest adminRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.username = "new-admin";
        request.password = "admin123";
        request.confirmPassword = "admin123";
        request.realName = "New Admin";
        request.role = Constants.ROLE_ADMIN;
        return request;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.username = username;
        request.password = password;
        return request;
    }

    private Student student(Long id, String studentNo, String name) {
        Student student = new Student();
        student.setId(id);
        student.setStudentNo(studentNo);
        student.setName(name);
        student.setGender("M");
        student.setPassword("123456");
        student.setMajorId(1L);
        student.setCollegeId(1L);
        student.setClassName("Class A");
        student.setStatus(1);
        return student;
    }

    private Teacher teacher(Long id, String teacherNo, String name) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setTeacherNo(teacherNo);
        teacher.setName(name);
        teacher.setGender("F");
        teacher.setPassword("123456");
        teacher.setDepartmentId(1L);
        teacher.setStatus(1);
        return teacher;
    }

    private Admin admin(Long id, String username) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setUsername(username);
        admin.setPassword("admin123");
        admin.setRole(3);
        admin.setStatus(1);
        return admin;
    }
}
