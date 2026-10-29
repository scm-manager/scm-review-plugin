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

import { defineConfig } from "vitest/config";
import { isCI } from "ci-info";
import path from "path";

export default defineConfig({
  // Mock ui-syntaxhighlighting module
  resolve: {
    alias: {
      "@scm-manager/ui-syntaxhighlighting": path.resolve(
        __dirname,
        "src/main/js/tests/mocks/ui-syntaxhighlighting.tsx",
      ),
    },
  },
  test: {
    globals: true,
    environment: "jsdom",
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
    exclude: ["**/node_modules/**", "**/dist/**", "**/.cache/**", "**/cypress/**"],
    reporters: ["default", "junit"],
    outputFile: {
      junit: "./build/jest-reports/TEST-vitest-report.xml",
    },
    coverage: {
      enabled: isCI,
      reporter: ["text", "lcov", "html"],
      provider: "v8",
      reportsDirectory: "./build/coverage",
      include: ["src/main/js/**/*.{ts,tsx}"],
    },
  },
});
