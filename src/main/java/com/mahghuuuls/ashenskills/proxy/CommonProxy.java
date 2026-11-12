package com.mahghuuuls.ashenskills.proxy;

/**
 * The only door from shared code to client-only code. Shared code calls methods declared here; the
 * dedicated server gets this class, whose methods do nothing, and the client gets the subclass in
 * the client package. Shared code must never name a client class directly.
 */
public class CommonProxy {
}
