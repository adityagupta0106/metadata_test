package com.serviceplus.metadata.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.serviceplus.metadata.payment.entity.PaymentConfiguration;

@Repository
public interface PaymentConfigurationRepository extends JpaRepository<PaymentConfiguration,Integer>{
	public PaymentConfiguration findByPaymentOptionId(Integer paymentOptionId);

}
