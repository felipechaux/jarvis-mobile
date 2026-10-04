package com.felipechaux.jarvis.core.network

/**
 * Version of the OpenJarvis mobile protocol this app speaks
 * (`openjarvis.mobile.PROTOCOL_VERSION` on the server).  Sent on pairing and
 * in the `hello` RPC; the server refuses versions outside its range so a
 * mismatch fails loudly instead of misbehaving.
 */
const val PROTOCOL_VERSION = 1

const val PAIRING_PAYLOAD_TYPE = "openjarvis-pair"
