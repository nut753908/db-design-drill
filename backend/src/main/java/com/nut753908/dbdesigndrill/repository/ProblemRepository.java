package com.nut753908.dbdesigndrill.repository;

import com.nut753908.dbdesigndrill.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
}
