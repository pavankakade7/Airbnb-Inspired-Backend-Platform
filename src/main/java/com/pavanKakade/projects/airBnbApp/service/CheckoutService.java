package com.pavanKakade.projects.airBnbApp.service;

import com.pavanKakade.projects.airBnbApp.entity.Booking;

public interface CheckoutService {

    String getCheckoutSession(Booking booking, String successUrl, String failureUrl);

}
