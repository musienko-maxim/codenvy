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
package com.codenvy.api.dao.mongo;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Set;

import static com.mongodb.MongoCredential.createCredential;
import static java.util.Collections.singletonList;

/**
 * Provides {@link MongoDatabase} instance.
 *
 * @author Eugene Voevodin
 */
public class MongoDatabaseProvider implements Provider<MongoDatabase> {

    private final MongoDatabase database;

    public MongoDatabaseProvider(String dbUrl,
                                 String dbName,
                                 String username,
                                 String password,
                                 Set<CodecProvider> codecProviders) {
        final MongoCredential credential = createCredential(username, dbName, password.toCharArray());
        final MongoClient mongoClient = new MongoClient(new ServerAddress(dbUrl), singletonList(credential));
        database = mongoClient.getDatabase(dbName).withCodecRegistry(CodecRegistries.fromProviders(new ArrayList<>(codecProviders)));
    }

    @Override
    public MongoDatabase get() {
        return database;
    }
}
