/*
 * Copyright (C) 2014 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.osiam.addons.self_administration.registration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osiam.addons.self_administration.exception.InvalidAttributeException;
import org.osiam.resources.scim.Address;
import org.osiam.resources.scim.Email;
import org.osiam.resources.scim.Extension;
import org.osiam.resources.scim.Im;
import org.osiam.resources.scim.Name;
import org.osiam.resources.scim.PhoneNumber;
import org.osiam.resources.scim.Photo;
import org.osiam.resources.scim.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

@Component
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
                .addEmails(getEmailList(registrationUser))
                .addPhoneNumbers(getPhoneNumberList(registrationUser))
                .addIms(getImList(registrationUser))
                .addPhotos(getPhotoList(registrationUser))
                .addAddresses(getAddressList(registrationUser))
                .addExtensions(getExtensions(registrationUser));

        return userBuilder.build();
    }

    private Set<Extension> getExtensions(RegistrationUser registrationUser) {
        Map<String, RegistrationExtension> registrationExtensions = registrationUser.getExtensions();
        Set<Extension> extensions = new HashSet<Extension>();
        for (Entry<String, RegistrationExtension> extensionSet : registrationExtensions.entrySet()) {
            Extension.Builder extensionBuilder = new Extension.Builder(extensionSet.getKey());

            RegistrationExtension currentRegistrationExtension = extensionSet.getValue();
            Map<String, String> registrationFields = currentRegistrationExtension.getFields();
            for (Entry<String, String> fieldSet : registrationFields.entrySet()) {
                extensionBuilder.setField(fieldSet.getKey(), fieldSet.getValue());
            }
            extensions.add(extensionBuilder.build());
        }
        return extensions;
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
            Im im = new Im.Builder().setValue(registrationUser.getIm())
                    .build();
            ims.add(im);
        }
        return ims;
    }

    private List<Photo> getPhotoList(RegistrationUser registrationUser) {
        List<Photo> photos = new ArrayList<Photo>();
        if (!Strings.isNullOrEmpty(registrationUser.getPhoto())) {
            Photo photo = null;
            try {
                photo = new Photo.Builder().setValue(new URI(registrationUser.getPhoto()))
                        .build();
            } catch (URISyntaxException e) {
                throw new InvalidAttributeException("Photo is not an URI", "registration.validation.photo", e);
            }
            photos.add(photo);
        }
        return photos;
    }

    private List<PhoneNumber> getPhoneNumberList(RegistrationUser registrationUser) {
        List<PhoneNumber> phoneNumbers = new ArrayList<PhoneNumber>();
        if (!Strings.isNullOrEmpty(registrationUser.getPhoneNumber())) {
            PhoneNumber phoneNumber = new PhoneNumber.Builder().setValue(registrationUser.getPhoneNumber())
                    .build();
            phoneNumbers.add(phoneNumber);
        }
        return phoneNumbers;
    }

    private List<Address> getAddressList(RegistrationUser registrationUser) {
        List<Address> addresses = new ArrayList<>();
        if (hasUserAddress(registrationUser)) {
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

    private boolean hasUserAddress(RegistrationUser registrationUser) {
        return Strings.isNullOrEmpty(registrationUser.getFormattedAddress())
                || Strings.isNullOrEmpty(registrationUser.getStreetAddress())
                || Strings.isNullOrEmpty(registrationUser.getLocality())
                || Strings.isNullOrEmpty(registrationUser.getRegion())
                || Strings.isNullOrEmpty(registrationUser.getPostalCode())
                || Strings.isNullOrEmpty(registrationUser.getCountry());
    }

}
