package com.meridian.platform.beacon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration.class)
@org.springframework.scheduling.annotation.EnableScheduling
public class BeaconNotificationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeaconNotificationsApplication.class, args);
    }
}
