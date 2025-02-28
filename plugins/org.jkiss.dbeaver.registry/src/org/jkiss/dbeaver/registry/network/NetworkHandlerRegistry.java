/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.registry.network;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.net.DBWHandlerRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NetworkHandlerRegistry implements DBWHandlerRegistry {
    private static NetworkHandlerRegistry instance = null;

    public synchronized static NetworkHandlerRegistry getInstance() {
        if (instance == null) {
            instance = new NetworkHandlerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<NetworkHandlerDescriptor> descriptors = new ArrayList<>();

    private NetworkHandlerRegistry(IExtensionRegistry registry) {
        // Load data descriptors from external plugins
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(NetworkHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                NetworkHandlerDescriptor formatterDescriptor = new NetworkHandlerDescriptor(ext);
                descriptors.add(formatterDescriptor);
            }

            // Remove replaced handlers
            for (NetworkHandlerDescriptor hd1 : descriptors) {
                for (NetworkHandlerDescriptor hd2 : descriptors) {
                    if (hd2.replaces(hd1)) {
                        hd1.setReplacedBy(hd2);
                        break;
                    }
                }
            }

            descriptors.sort(Comparator.comparingInt(NetworkHandlerDescriptor::getOrder));
        }
    }

    @NotNull
    public List<NetworkHandlerDescriptor> getDescriptors() {
        List<NetworkHandlerDescriptor> descList = new ArrayList<>(descriptors);
        descList.removeIf(nhd -> nhd.getReplacedBy() != null);
        return descList;
    }

    public NetworkHandlerDescriptor getDescriptor(@NotNull String id) {
        for (NetworkHandlerDescriptor descriptor : descriptors) {
            if (descriptor.getId().equals(id)) {
                if (descriptor.getReplacedBy() != null) {
                    return descriptor.getReplacedBy();
                }
                return descriptor;
            }
        }
        return null;
    }

    @NotNull
    public List<NetworkHandlerDescriptor> getDescriptors(@NotNull DBPDataSourceContainer dataSource) {
        return getDescriptors(dataSource.getDriver());
    }

    @NotNull
    public List<NetworkHandlerDescriptor> getDescriptors(@NotNull DBPDriver driver) {
        List<NetworkHandlerDescriptor> result = new ArrayList<>();
        for (NetworkHandlerDescriptor d : descriptors) {
            if (d.getReplacedBy() == null && !d.hasObjectTypes() || d.matches(driver)) {
                result.add(d);
            }
        }
        return result;
    }

}
