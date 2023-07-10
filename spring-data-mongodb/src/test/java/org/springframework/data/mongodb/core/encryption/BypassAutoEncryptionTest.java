/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.mongodb.core.encryption;

import java.util.Collections;

import org.bson.BsonBinary;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.PropertyValueConverterFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.MongoConverterConfigurationAdapter;
import org.springframework.data.mongodb.core.convert.encryption.MongoEncryptionConverter;
import org.springframework.data.mongodb.core.encryption.BypassAutoEncryptionTest.Config;
import org.springframework.data.util.Lazy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.mongodb.AutoEncryptionSettings;
import com.mongodb.ClientEncryptionSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryptions;

/**
 * Encryption tests for client having {@link AutoEncryptionSettings#isBypassAutoEncryption()}.
 *
 * @author Christoph Strobl
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Config.class)
public class BypassAutoEncryptionTest extends AbstractEncryptionTestBase {

	@Disabled
	@Override
	void altKeyDetection(@Autowired CachingMongoClientEncryption mongoClientEncryption) throws InterruptedException {
		super.altKeyDetection(mongoClientEncryption);
	}

	@Configuration
	static class Config extends EncryptionConfig {

		@Autowired ApplicationContext applicationContext;

		@Override
		protected void configureClientSettings(Builder builder) {

			MongoClient mongoClient = MongoClients.create();
			ClientEncryptionSettings clientEncryptionSettings = encryptionSettings(mongoClient);
			mongoClient.close();

			builder.autoEncryptionSettings(AutoEncryptionSettings.builder() //
					.kmsProviders(clientEncryptionSettings.getKmsProviders()) //
					.keyVaultNamespace(clientEncryptionSettings.getKeyVaultNamespace()) //
					.bypassAutoEncryption(true).build());
		}

		@Override
		protected void configureConverters(MongoConverterConfigurationAdapter converterConfigurationAdapter) {

			converterConfigurationAdapter
					.registerPropertyValueConverterFactory(PropertyValueConverterFactory.beanFactoryAware(applicationContext));
		}

		@Bean
		@Override
		MongoEncryptionConverter encryptingConverter(MongoClientEncryption mongoClientEncryption) {

			Lazy<BsonBinary> dataKey = Lazy.of(() -> mongoClientEncryption.getClientEncryption().createDataKey("local",
					new DataKeyOptions().keyAltNames(Collections.singletonList("mySuperSecretKey"))));

			return new MongoEncryptionConverter(mongoClientEncryption,
					EncryptionKeyResolver.annotated((ctx) -> EncryptionKey.keyId(dataKey.get())));
		}

		@Bean
		@Override
		CachingMongoClientEncryption clientEncryption(ClientEncryptionSettings encryptionSettings) {
			return new CachingMongoClientEncryption(() -> ClientEncryptions.create(encryptionSettings));
		}

	}

}
