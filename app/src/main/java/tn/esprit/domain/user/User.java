package tn.esprit.domain.user;

/**
 * Domain model for the authenticated user, matches backend dto.user.UserDto.
 *
 * This class is also used as the Retrofit response model (no extra mapping).
 */
public class User {

    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String profileImage;
    private Boolean isFirstLogin;
    private String role;
    private String status;
    private String createdAt;
    private String updatedAt;

    // Needed by Gson / Retrofit
    public User() {
    }

    public User(Long id,
                String firstname,
                String lastname,
                String email,
                String phone,
                String profileImage,
                Boolean isFirstLogin,
                String role,
                String status,
                String createdAt,
                String updatedAt) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
        this.profileImage = profileImage;
        this.isFirstLogin = isFirstLogin;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public Boolean getFirstLogin() {
        return isFirstLogin;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
