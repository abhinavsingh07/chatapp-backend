package com.chatapp.synk.initialize;

import com.chatapp.synk.entity.User;
import com.chatapp.synk.entity.UserRole;
import com.chatapp.synk.enums.RoleName;
import com.chatapp.synk.repository.UserRepository;
import com.chatapp.synk.repository.UserRoleRepository;
import com.chatapp.synk.util.RandomUUIDGenerater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    public PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) {
        logger.info("Running DataInitializer to set up default roles and admin user");

        //create default roles in useroles table if missing
        // Check and insert ROLE_USER
        if (userRoleRepository.findByName(RoleName.ROLE_USER).isEmpty()) {
            String id = RandomUUIDGenerater.getId(UserRole.ALIAS_USER_ROLE).toString();
            userRoleRepository.save(new UserRole(id, RoleName.ROLE_USER));
        }

        // Check and insert ROLE_ADMIN
        if (userRoleRepository.findByName(RoleName.ROLE_ADMIN).isEmpty()) {
            String id = RandomUUIDGenerater.getId(UserRole.ALIAS_USER_ROLE).toString();
            userRoleRepository.save(new UserRole(id, RoleName.ROLE_ADMIN));
        }

        // 2. Create default admin user if missing
        String adminEmail = "admin@chatapp.com";
        String adminPhone = "1234567890"; // Default phone number, change in prod!
        String adminName = "Admin User"; // Default username, change in prod!
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            logger.info("Creating default admin user");
            User adminUser = new User();
            adminUser.setId(RandomUUIDGenerater.getId(User.ALIAS_USER).toString());
            adminUser.setPhoneNumber(adminPhone);
            adminUser.setEmail(adminEmail);
            adminUser.setName(adminName);
            adminUser.setPassword(passwordEncoder.encode("admin123")); // Change in prod!
            adminUser.setUserRole(RoleName.ROLE_ADMIN); // Only admin role

            userRepository.save(adminUser);
            System.out.println("Default admin user created: " + adminName);
        }
    }
}
