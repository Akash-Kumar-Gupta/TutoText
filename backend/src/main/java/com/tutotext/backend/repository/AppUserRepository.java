package com.tutotext.backend.repository;

import com.tutotext.backend.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
	boolean existsByEmail(String email);

	Optional<AppUser> findByEmail(String email);
}
