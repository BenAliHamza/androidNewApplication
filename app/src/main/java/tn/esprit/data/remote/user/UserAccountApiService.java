package tn.esprit.data.remote.user;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.PUT;

import tn.esprit.domain.user.User;

/**
 * API for base user information (firstname, lastname, email, phone).
 *
 * Uses:
 *   PUT /me
 */
public interface UserAccountApiService {

    class UserUpdateRequestDto {
        private String firstname;
        private String lastname;
        private String email;
        private String phone;

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    @PUT("/me")
    Call<User> updateCurrentUser(
            @Header("Authorization") String authHeader,
            @Body UserUpdateRequestDto body
    );
}
