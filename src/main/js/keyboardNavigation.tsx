import React, { FC, useCallback, useContext, useEffect, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useShortcut } from "@scm-manager/ui-components";

type CallbackMap = {
  [index: number]: () => void;
};

const getMaxIndex = (callbacks: CallbackMap) =>
  Object.keys(callbacks).reduce((prev, max) => (prev > Number(max) ? prev : Number(max)), 0);

type KeyboardNavigationContextType = {
  register: (index: number, callback: () => void) => void;
  deregister: (index: number) => void;
};

const KeyboardNavigationContext = React.createContext({} as KeyboardNavigationContextType);

export const KeyboardNavigationContextProvider: FC = ({ children }) => {
  const [t] = useTranslation("plugins");
  const callbacks = useRef<CallbackMap>({});
  const activeIndex = useRef<number>(-1);
  const value = useMemo(
    () => ({
      register: (index: number, callback: () => void) => (callbacks.current[index] = callback),
      deregister: (index: number) => {
        delete callbacks.current[index];
      }
    }),
    []
  );
  const executeCallback = useCallback((index: number) => callbacks.current[index](), []);
  const navigateBackward = useCallback(() => {
    if (activeIndex.current === -1) {
      activeIndex.current = 0;
    } else if (activeIndex.current > 0) {
      activeIndex.current -= 1;
    }
    executeCallback(activeIndex.current);
  }, [executeCallback]);
  const navigateForward = useCallback(() => {
    if (activeIndex.current === -1) {
      activeIndex.current = 0;
    } else if (activeIndex.current < getMaxIndex(callbacks.current)) {
      activeIndex.current += 1;
    }
    executeCallback(activeIndex.current);
  }, [executeCallback]);
  useShortcut("j", navigateBackward, {
    description: t("scm-review-plugin.shortcuts.previousPullRequest")
  });
  useShortcut("k", navigateForward, {
    description: t("scm-review-plugin.shortcuts.nextPullRequest")
  });
  useShortcut("tab", () => {
    activeIndex.current = -1;

    return true;
  });
  return <KeyboardNavigationContext.Provider value={value}>{children}</KeyboardNavigationContext.Provider>;
};

export function useKeyboardNavigationTarget(index: number, callback: () => void) {
  const { register, deregister } = useContext(KeyboardNavigationContext);
  useEffect(() => {
    register(index, callback);
    return () => deregister(index);
  }, [index, callback, register, deregister]);
}
