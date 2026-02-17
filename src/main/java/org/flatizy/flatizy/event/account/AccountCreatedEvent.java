package org.flatizy.flatizy.event.account;

import java.util.List;

public record AccountCreatedEvent(List<Integer> accountId) {}
