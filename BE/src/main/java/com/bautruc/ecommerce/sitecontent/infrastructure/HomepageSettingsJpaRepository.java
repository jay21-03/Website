package com.bautruc.ecommerce.sitecontent.infrastructure;

import com.bautruc.ecommerce.sitecontent.domain.HomepageSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomepageSettingsJpaRepository extends JpaRepository<HomepageSettings, Short> {
}
