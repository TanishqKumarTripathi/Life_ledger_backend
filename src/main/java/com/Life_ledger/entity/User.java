package com.Life_ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    private String phoneNumber;

    private String profilePicUrl;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<BankAccount> bankAccounts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<FileImport> fileImports;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Goal> goals;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserCorrection> corrections;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; // or add roles if needed
    }

    @Override
    public String getUsername() {
        return email; // Spring Security will use email as username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getPassword() {
        return password;
    }

}
