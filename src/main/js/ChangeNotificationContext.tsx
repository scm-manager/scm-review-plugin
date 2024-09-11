/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

import React, { FC, useContext } from "react";

type Callback = () => void;

type SubscriptionContextType = {
  onReload: (callback: Callback) => () => void;
  reload: () => void;
};

const createSubscriptionContext = (): SubscriptionContextType => {
  const listeners: Callback[] = [];
  return {
    onReload: (callback: Callback) => {
      const idx = listeners.push(callback);
      return () => listeners.splice(idx, 1);
    },
    reload: () => {
      listeners.forEach(c => c());
    }
  };
};

const defaultSubscriptionContext = createSubscriptionContext();

const Context = React.createContext(defaultSubscriptionContext);

const ChangeNotificationContext: FC = ({ children }) => (
  <Context.Provider value={defaultSubscriptionContext}>{children}</Context.Provider>
);

export const useChangeNotificationContext = () => {
  return useContext(Context);
};

export default ChangeNotificationContext;
