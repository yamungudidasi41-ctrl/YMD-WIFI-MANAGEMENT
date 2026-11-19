package com.ymd.wifi.repository;
import com.ymd.wifi.model.WifiPackage;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PackageRepository extends JpaRepository<WifiPackage, Long> {}
