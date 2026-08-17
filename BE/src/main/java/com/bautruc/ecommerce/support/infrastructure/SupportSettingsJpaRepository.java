package com.bautruc.ecommerce.support.infrastructure;

import com.bautruc.ecommerce.support.domain.SupportSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportSettingsJpaRepository extends JpaRepository<SupportSettings, Short> {
}
