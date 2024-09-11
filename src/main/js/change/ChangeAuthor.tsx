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

import React, { FC, PropsWithChildren } from "react";
import { ContributorAvatar } from "@scm-manager/ui-components";
import { Person } from "@scm-manager/ui-types";
import { extensionPoints, useBinder } from "@scm-manager/ui-extensions";

type MailToProps = {
  mail?: string;
};

const MailTo: FC<PropsWithChildren<MailToProps>> = ({ mail, children }) => {
  return mail ? <a href={`mailto:${mail}`}>{children}</a> : <>children</>;
};

const ChangeAuthor: FC<{ person: Person }> = ({ person }) => {
  const binder = useBinder();
  const avatarFactory = binder.getExtension<extensionPoints.AvatarFactory>("avatar.factory");
  const avatar = avatarFactory ? avatarFactory(person) : null;

  return (
    <MailTo mail={person.mail}>
      {avatar ? <ContributorAvatar src={avatar} alt={person.name}></ContributorAvatar> : null}
      {avatar ? " " : null}
      {person.name}
    </MailTo>
  );
};

export default ChangeAuthor;
