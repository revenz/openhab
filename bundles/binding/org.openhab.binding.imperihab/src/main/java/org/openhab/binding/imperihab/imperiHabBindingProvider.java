/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.imperihab;

import java.util.ArrayList;
import java.util.HashMap;

import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;

/**
 * @author reven
 * @since 1.7.0
 */
public interface imperiHabBindingProvider extends BindingProvider {

	ArrayList<String> getRooms();

	ArrayList<String> getDevices(HashMap<String, Item> items);

}
