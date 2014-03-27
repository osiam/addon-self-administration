package org.osiam.addons.selfadministration.registration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.osiam.addons.selfadministration.exception.InvalidAttributeException;
import org.osiam.resources.scim.Address;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.Extension;
import org.osiam.resources.scim.Im;
import org.osiam.resources.scim.Name;
import org.osiam.resources.scim.PhoneNumber;
import org.osiam.resources.scim.Photo;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

@Service
public class UserConverter {

    @Value("${org.osiam.html.form.usernameEqualsEmail:true}")
    private boolean usernameEqualsEmail;

    public User toScimUser(RegistrationUser registrationUser) {

        Name name = new Name.Builder()
                .setFormatted(registrationUser.getFormattedName())
                .setFamilyName(registrationUser.getFamilyName())
                .setGivenName(registrationUser.getGivenName())
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

        User.Builder userBuilder;
        if (usernameEqualsEmail) {
            userBuilder = new User.Builder(registrationUser.getEmail());
        } else {
            userBuilder = new User.Builder(registrationUser.getUserName());
        }

        userBuilder
        .setName(name)
        .setDisplayName(registrationUser.getDisplayName())
        .setNickName(registrationUser.getNickName())
        .setProfileUrl(registrationUser.getProfileUrl())
        .setTitle(registrationUser.getTitle())
        .setPreferredLanguage(registrationUser.getPreferredLanguage())
        .setLocale(registrationUser.getLocale())
        .setTimezone(registrationUser.getTimezone())
        .setPassword(registrationUser.getPassword())
        .setEmails(getEmailList(registrationUser))
        .setPhoneNumbers(getPhoneNumberList(registrationUser))
        .setIms(getImList(registrationUser))
        .setPhotos(getPhotoList(registrationUser))
        .setAddresses(getAddressList(registrationUser));

        return userBuilder.build();
    }

    private void getExtensions(RegistrationUser registrationUser){
        HashMap<String, Extension> extensionMap = new HashMap<>();
        
        
        for (String key : registrationUser.getExtensions().keySet()) {
            
        }
    }
    
    private List<Email> getEmailList(RegistrationUser registrationUser) {
        List<Email> emails = new ArrayList<Email>();
        if (!Strings.isNullOrEmpty(registrationUser.getEmail())) {
            Email email = new Email.Builder().setValue(registrationUser.getEmail())
                    .build();
            emails.add(email);
        }
        return emails;
    }

    private List<Im> getImList(RegistrationUser registrationUser) {
        List<Im> ims = new ArrayList<Im>();
        if (!Strings.isNullOrEmpty(registrationUser.getIm())) {
            Im phoenNumber = new Im.Builder().setValue(registrationUser.getIm())
                    .build();
            ims.add(phoenNumber);
        }
        return ims;
    }

    private List<Photo> getPhotoList(RegistrationUser registrationUser) {
        List<Photo> photos = new ArrayList<Photo>();
        if (!Strings.isNullOrEmpty(registrationUser.getPhoto())) {
            Photo phoenNumber = null;
            try {
                phoenNumber = new Photo.Builder().setValue(new URI(registrationUser.getPhoto()))
                        .build();
            } catch (URISyntaxException e) {
                throw new InvalidAttributeException("Photo is not an URI", "registration.exception.photo");
            }
            photos.add(phoenNumber);
        }
        return photos;
    }

    private List<PhoneNumber> getPhoneNumberList(RegistrationUser registrationUser) {
        List<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>();
        if (!Strings.isNullOrEmpty(registrationUser.getPhoneNumber())) {
            PhoneNumber phoenNumber = new PhoneNumber.Builder().setValue(registrationUser.getPhoneNumber())
                    .build();
            phoneNumbers.add(phoenNumber);
        }
        return phoneNumbers;
    }

    private List<Address> getAddressList(RegistrationUser registrationUser) {
        List<Address> addresses = new ArrayList<>();
        if (Strings.isNullOrEmpty(registrationUser.getFormattedAddress())
                || Strings.isNullOrEmpty(registrationUser.getStreetAddress())
                || Strings.isNullOrEmpty(registrationUser.getLocality())
                || Strings.isNullOrEmpty(registrationUser.getRegion())
                || Strings.isNullOrEmpty(registrationUser.getPostalCode())
                || Strings.isNullOrEmpty(registrationUser.getCountry())) {

            Address address = new Address.Builder()
                    .setFormatted(registrationUser.getFormattedAddress())
                    .setStreetAddress(registrationUser.getStreetAddress())
                    .setLocality(registrationUser.getLocality())
                    .setRegion(registrationUser.getRegion())
                    .setPostalCode(registrationUser.getPostalCode())
                    .setCountry(registrationUser.getCountry())
                    .build();
            addresses.add(address);
        }
        return addresses;
    }

}
