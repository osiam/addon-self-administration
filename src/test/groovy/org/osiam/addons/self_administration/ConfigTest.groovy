package org.osiam.addons.self_administration

import org.osiam.addons.self_administration.registration.HtmlField
import spock.lang.Specification

class ConfigTest extends Specification {

    def "CreateAllAllowedFields"() {

        given:
        String[] fields = ['userName:true:number', 'familyName:false', 'middleName:true', 'im']
        String[] extensions = ['extensions["urn:client:extension"].fields["age"]:false',
                               'extensions["urn:client:extension"].fields["age"]:true:number']

        Config config = new Config()

        when:
        config.createAllAllowedFields(fields, extensions)

        then:
        HtmlField[] fields1 = config.getAllAllowedFields()
        fields1.length == 8
    }
}
