package org.osiam.addons.selfadministration.registration;

import java.util.ArrayList;
import java.util.List;

import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.Name;
import org.osiam.resources.scim.PhoneNumber;
import org.osiam.resources.scim.User;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

@Service
public class UserConverter {

    public User toScimUser(RegistrationUser registrationUser) {

        Name name = new Name.Builder()
                .setFamilyName(registrationUser.getFamilyName())
                .setFamilyName(registrationUser.getFamilyName())
                .setMiddleName(registrationUser.getMiddleName())
                .setHonorificPrefix(registrationUser.getHonorificPrefix())
                .setHonorificSuffix(registrationUser.getHonorificSuffix())
                .build();

        List<Email> emails = new ArrayList<Email>();
        if (!Strings.isNullOrEmpty(registrationUser.getEmail())) {
            Email email = new Email.Builder().setValue(registrationUser.getEmail())
                    .build();
            emails.add(email);
        }

        List<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>();
        if (!Strings.isNullOrEmpty(registrationUser.getPhoneNumber())) {
            PhoneNumber phoneNumber = new PhoneNumber.Builder().setValue(registrationUser.getPhoneNumber())
                    .build();
            phoneNumbers.add(phoneNumber);
        }
        
        User.Builder userBuilder = new User.Builder(registrationUser.getUserName())

                .setPassword(registrationUser.getPassword())
                .setName(name)
                .setDisplayName(registrationUser.getDisplayName())
                .setNickName(registrationUser.getNickName())
                .setTitle(registrationUser.getTitle())
                .setPreferredLanguage(registrationUser.getPreferredLanguage())
                .setLocale(registrationUser.getLocale())
                .setTimezone(registrationUser.getTimezone())
                .setEmails(emails)
                .setPhoneNumbers(phoneNumbers);

        return userBuilder.build();
    }
}
