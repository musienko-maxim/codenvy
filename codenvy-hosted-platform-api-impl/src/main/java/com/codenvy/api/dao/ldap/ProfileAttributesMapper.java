/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.dao.ldap;

import org.eclipse.che.api.user.server.dao.Profile;
import org.eclipse.che.commons.lang.Pair;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.ModificationItem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static javax.naming.directory.DirContext.ADD_ATTRIBUTE;
import static javax.naming.directory.DirContext.REMOVE_ATTRIBUTE;
import static javax.naming.directory.DirContext.REPLACE_ATTRIBUTE;

/**
 * Mapper is used for mapping LDAP Attributes to {@link Profile}.
 *
 * @author Eugene Voevodin
 */
@Singleton
public class ProfileAttributesMapper {

    private final String              profileDn;
    private final String              profileIdAttr;
    private final Map<String, String> allowedAttributes;

    /**
     * Creates new instance of {@link ProfileAttributesMapper}
     * <p/>
     * Note that only {@code allowedAttributes} going to be fetched from ldap storage.
     * Assuming we have profile which contains following ldap attributes:
     * <pre>
     *      ...
     *      "sn" : "Any User"
     *      "telephoneNumber": "Users phone number"
     *      "description" : "Ldap user"
     *      ...
     * </pre>
     * if {@code allowedAttributes} contains such pairs as <i>("sn", "name"), ("telephoneNumber", "phone")</i>
     * then returned profile will contain following attributes:
     * <pre>
     *      "name"  : "Any User"
     *      "phone" : "Users phone number"
     * </pre>
     *
     * @param profileDn
     *         name of attribute that contains name of profile object. Typical value is 'cn'
     * @param profileIdAttr
     *         name of attribute that contains profile identifier. Typical value is 'uid'
     * @param allowedAttributes
     *         attributes which will be fetched from ldap storage
     */
    @Inject
    public ProfileAttributesMapper(@Named("profile.ldap.profile_dn") String profileDn,
                                   @Named("profile.ldap.attr.id") String profileIdAttr,
                                   @Named("profile.ldap.allowed_attributes") Pair<String, String>[] allowedAttributes) {
        this.profileDn = profileDn;
        this.profileIdAttr = profileIdAttr;
        this.allowedAttributes = new HashMap<>();
        for (Pair<String, String> pair : allowedAttributes) {
            this.allowedAttributes.put(pair.first, pair.second);
        }
    }

    public Profile asProfile(Attributes attributes) throws NamingException {
        final String id = attributes.get(profileIdAttr).get().toString();
        return new Profile().withId(id)
                            .withUserId(id)
                            .withAttributes(asMap(attributes.getAll()));
    }

    public String formatDn(String id, String containerDn) {
        return profileDn + '=' + id + ',' + containerDn;
    }

    public ModificationItem[] createModifications(Map<String, String> existing, Map<String, String> update) {
        //reverse allowed attributes
        final Map<String, String> reversed = reverse(allowedAttributes);

        //remove not allowed attributes from update
        for (Iterator<Map.Entry<String, String>> it = update.entrySet().iterator(); it.hasNext(); ) {
            if (!reversed.containsKey(it.next().getKey())) {
                it.remove();
            }
        }

        final List<ModificationItem> mods = new LinkedList<>();
        //preparing 'remove' & 'replace' modifications
        for (Map.Entry<String, String> entry : existing.entrySet()) {
            final String updateValue = update.get(entry.getKey());
            if (updateValue == null) {
                mods.add(new ModificationItem(REMOVE_ATTRIBUTE, new BasicAttribute(reversed.get(entry.getKey()), entry.getValue())));
            } else if (!updateValue.equals(entry.getValue())) {
                mods.add(new ModificationItem(REPLACE_ATTRIBUTE, new BasicAttribute(reversed.get(entry.getKey()), updateValue)));
            }
        }
        //preparing 'add' modifications
        for (Map.Entry<String, String> entry : update.entrySet()) {
            if (!existing.containsKey(entry.getKey())) {
                mods.add(new ModificationItem(ADD_ATTRIBUTE, new BasicAttribute(reversed.get(entry.getKey()), entry.getValue())));
            }
        }
        return mods.toArray(new ModificationItem[mods.size()]);
    }

    private Map<String, String> asMap(NamingEnumeration<? extends Attribute> enumeration) throws NamingException {
        final Map<String, String> attributes = new HashMap<>();
        try {
            while (enumeration.hasMore()) {
                final Attribute attribute = enumeration.next();
                if (allowedAttributes.containsKey(attribute.getID())) {
                    attributes.put(allowedAttributes.get(attribute.getID()), attribute.get().toString());
                }
            }
        } finally {
            enumeration.close();
        }
        return attributes;
    }

    private Map<String, String> reverse(Map<String, String> src) {
        final Map<String, String> reversed = new HashMap<>();
        for (Map.Entry<String, String> entry : src.entrySet()) {
            reversed.put(entry.getValue(), entry.getKey());
        }
        return reversed;
    }
}