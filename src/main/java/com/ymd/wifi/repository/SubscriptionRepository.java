package com.ymd.wifi.repository;
import com.ymd.wifi.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findAllByOrderByExpiryDateAsc();
}
