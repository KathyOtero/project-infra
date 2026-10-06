package com.caribexperience.repository;

import com.caribexperience.domain.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CiudadRepository extends JpaRepository<Ciudad, Long> {
}
