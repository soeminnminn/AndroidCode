package com.s16.utils

import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.math.pow

// https://github.com/marcelkliemannel/kotlin-onetimepassword/

/**
 * Available "keyed-hash message authentication code" (HMAC) algorithms.
 * See: https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#Mac
 *
 * @property macAlgorithmName the name of the algorithm used for
 *                            [javax.crypto.Mac.getInstance(java.lang.String)]
 * @property hashBytes the length of the returned hash produced by the algorithm.
 */
enum class HmacAlgorithm(val macAlgorithmName: String, val hashBytes: Int) {
    /**
     * SHA1 HMAC with a hash of 20-bytes
     */
    SHA1("HmacSHA1", 20),
    /**
     * SHA256 HMAC with a hash of 32-bytes
     */
    SHA256("HmacSHA256", 32),
    /**
     * SHA512 HMAC with a hash of 64-bytes
     */
    SHA512("HmacSHA512", 64);
}

/**
 * The configuration for the [HmacOneTimePasswordGenerator].
 *
 * @property codeDigits the length of the generated code. The RFC 4226 requires
 *                      a code digits value between 6 and 8, to assure a good
 *                      security trade-off. However, this library does not set
 *                      any requirement for this property. But notice that through
 *                      the design of the algorithm the maximum code value is
 *                      2,147,483,647. Which means that a larger code digits value
 *                      than 10 just adds more trailing zeros to the code (and in
 *                      case of 10 digits the first number is always 0, 1 or 2).
 *
 * @property hmacAlgorithm the "keyed-hash message authentication code" algorithm
 *                         to use to generate the hash, from which the code is
 *                         extracted (see [HmacAlgorithm] for available algorithms).
 */
open class HmacOneTimePasswordConfig(var codeDigits: Int, var hmacAlgorithm: HmacAlgorithm)

/**
 * Generator for the RFC 4226 "HOTP: An HMAC-Based One-Time Password Algorithm"
 * (https://tools.ietf.org/html/rfc4226)
 *
 * @property secret the shared secret as a byte array.
 * @property config the configuration for this generator.
 */
class HmacOneTimePasswordGenerator(private val secret: ByteArray, private val config: HmacOneTimePasswordConfig) {
    /**
     * Generated a code as a HOTP one-time password.
     *
     * @return The generated code for the provided counter value. Note, that the
     *         code must be represented as a string because it can have trailing
     *         zeros to meet the code digits requirement from the configuration.
     */
    fun generate(counter: Long): String {
        if (config.codeDigits <= 0) {
            return ""
        }

        // The counter value is the input parameter 'message' to the HMAC algorithm.
        // It must be  represented by a byte array with the length of a long (8 byte).
        //
        // Ongoing example:
        // counter = 1234
        // message = [0, 0, 0, 0, 0, 0, 4, -46]
        val message: ByteBuffer = ByteBuffer.allocate(8).putLong(0, counter)

        // Compute the HMAC hash with the algorithm, 'secret' and 'message' as input parameter.
        //
        // Ongoing example:
        // secret = "Leia"
        // algorithm = "HmacSHA1"
        // hash = [-1, 12, -126, -80, -86, 107, 104, -30, -14, 83, 77, -97, -42, -5, 121, -101, 82, -104, 65, -59]
        val hash = Mac.getInstance(config.hmacAlgorithm.macAlgorithmName).run {
            init(SecretKeySpec(secret, "RAW")) // The hard-coded value 'RAW' is specified in the RFC
            doFinal(message.array())
        }

        // The value of the offset is the lower 4 bits of the last byte of the hash
        // (0x0F = 0000 1111).
        //
        // Ongoing example:
        // The last byte of the hash  is at index 19 and has the value -59. The value
        // of the lower 4 bits of -59 is 5.
        val offset = hash.last().and(0x0F).toInt()

        // The first step for extracting the binary value is to collect the next four
        // bytes from the hash, starting at the index of the offset.
        //
        // Ongoing example:
        // Starting at offset 5, the binary value is [107, 104, -30, -14].
        val binary = ByteBuffer.allocate(4).apply {
            for (i in 0..3) {
                put(i, hash[i + offset])
            }
        }

        // The second step is to drop the most significant bit (MSB) from the first
        // step binary value (0x7F = 0111 1111).
        //
        // Ongoing example:
        // The value at index 0 is 107, which has a MSB of 0. So nothing must be done
        // and the binary value remains the same.
        binary.put(0, binary.get(0).and(0x7F))

        // The resulting integer value of the code must have at most the required code
        // digits. Therefore the binary value is reduced by calculating the modulo
        // 10 ^ codeDigits.
        //
        // On going example:
        // binary = [107, 104, -30, -14] = 137359152
        // codeDigits = 6
        // codeInt = 137359152 % 10^6 = 35954
        val codeInt = binary.int.rem(10.0.pow(config.codeDigits).toInt())

        // The integer code variable may contain a value with fewer digits than the
        // required code digits. Therefore the final code value is filled with zeros
        // on the left, till the code digits requirement is fulfilled.
        //
        // Ongoing example:
        // The current value of the 'oneTimePassword' variable has 5 digits. Therefore
        // the resulting code is filled with one 0 at the beginning, to meet the 6
        // digits requirement.
        var codeString = codeInt.toString()
        repeat(config.codeDigits - codeString.length) {
            codeString = "0$codeString"
        }

        return codeString
    }

    /**
     * Validates the given code.
     *
     * @param code the code calculated from the challenge to validate.
     * @param counter the used challenge for the code.
     */
    fun isValid(code: String, counter: Long): Boolean {
        return code == generate(counter)
    }
}

/**
 * The configuration for the [TimeBasedOneTimePasswordGenerator].
 *
 * @property timeStep represents together with the [timeStepUnit] parameter the
 *                    time range in which the challenge is valid (e.g. 30 seconds).
 * @property timeStepUnit see [timeStep]
 * @property codeDigits see documentation in [HmacOneTimePasswordConfig].
 * @property hmacAlgorithm see documentation in [HmacOneTimePasswordConfig].
 */
open class TimeBasedOneTimePasswordConfig(val timeStep: Long,
                                          val timeStepUnit: TimeUnit,
                                          codeDigits: Int,
                                          hmacAlgorithm: HmacAlgorithm
): HmacOneTimePasswordConfig(codeDigits, hmacAlgorithm)

/**
 * Generator for the RFC 6238 "TOTP: Time-Based One-Time Password Algorithm"
 * (https://tools.ietf.org/html/rfc6238)
 *
 * @property secret the shared secret as a byte array.
 * @property config the configuration for this generator.
 */
open class TimeBasedOneTimePasswordGenerator(private val secret: ByteArray, private val config: TimeBasedOneTimePasswordConfig){
    private val hmacOneTimePasswordGenerator: HmacOneTimePasswordGenerator =
        HmacOneTimePasswordGenerator(secret, config)

    /**
     * Generated a code as a TOTP one-time password.
     *
     * @param timestamp the challenge for the code. The default value is the
     *                  current system time from [System.currentTimeMillis].
     */
    fun generate(timestamp: Date = Date(System.currentTimeMillis())): String {
        val counter = if (config.timeStep == 0L) {
            0 // To avoide a divide by zero exception
        }
        else {
            timestamp.time.div(TimeUnit.MILLISECONDS.convert(config.timeStep, config.timeStepUnit))
        }

        return hmacOneTimePasswordGenerator.generate(counter)
    }

    /**
     * Validates the given code.
     *
     * @param code the code calculated from the challenge to validate.
     * @param timestamp the used challenge for the code. The default value is the
     *                  current system time from [System.currentTimeMillis].
     */
    fun isValid(code: String, timestamp: Date = Date(System.currentTimeMillis())): Boolean {
        return code == generate(timestamp)
    }
}