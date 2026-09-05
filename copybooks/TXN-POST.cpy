      *****************************************************************
      * TXN-POST - BEDROCK TRANSACTION POSTING, CICS TRANSACTION MTTP *
      *                                                               *
      * REQUEST LENGTH 160, RESPONSE LENGTH 96.                       *
      * SENT BY TXN-POSTING-SERVICE THROUGH BEDROCK-ADAPTER-SERVICE.  *
      *                                                               *
      * POST-IDEMPOTENCY-KEY IS THE DIGITAL SIDE KEY. BEDROCK KEEPS   *
      * THE LAST 30 DAYS OF KEYS AND RETURNS 04 WITH THE ORIGINAL     *
      * TRAN-ID FOR A DUPLICATE, WHICH THE CALLER MUST TREAT AS       *
      * SUCCESS. THIS IS NOT AN ERROR. SEE INC0047019.                *
      *                                                               *
      * A REVERSAL IS A POST WITH POST-TYPE 'R' AND POST-ORIG-TRAN-ID *
      * SET. AMOUNT MUST EQUAL THE ORIGINAL; PARTIAL REVERSALS ARE    *
      * NOT SUPPORTED BY BEDROCK AND COME BACK AS 08.                 *
      *                                                               *
      * RETURN CODES                                                  *
      *   00  POSTED                                                  *
      *   04  DUPLICATE IDEMPOTENCY KEY, ORIGINAL RETURNED            *
      *   08  REJECTED, SEE REASON                                    *
      *   12  BEDROCK UNAVAILABLE (EOD BATCH RUNNING)                 *
      *   16  ABEND, SEE ABEND CODE                                   *
      *****************************************************************
       01  TXN-POST-REQUEST.
           05  POST-HDR.
               10  POST-TRAN-CODE       PIC X(04) VALUE 'MTTP'.
               10  POST-CORRELATION-ID  PIC X(32).
               10  POST-CHANNEL         PIC X(04).
           05  POST-IDEMPOTENCY-KEY     PIC X(36).
           05  POST-TYPE                PIC X(01).
               88  POST-TYPE-DEBIT          VALUE 'D'.
               88  POST-TYPE-CREDIT         VALUE 'C'.
               88  POST-TYPE-REVERSAL       VALUE 'R'.
           05  POST-ACCT-ID             PIC X(16).
           05  POST-AMOUNT              PIC S9(13)  DISPLAY SIGN
                                        TRAILING INCLUDED.
           05  POST-ORIG-TRAN-ID        PIC X(16).
           05  POST-DESCRIPTION         PIC X(32).
           05  FILLER                   PIC X(06).
       01  TXN-POST-RESPONSE.
           05  PRSP-HDR.
               10  PRSP-TRAN-CODE       PIC X(04).
               10  PRSP-CORRELATION-ID  PIC X(32).
               10  PRSP-RETURN-CODE     PIC 9(02).
               10  PRSP-ABEND-CODE      PIC X(04).
           05  PRSP-TRAN-ID             PIC X(16).
           05  PRSP-NEW-BALANCE         PIC S9(13)  DISPLAY SIGN
                                        TRAILING INCLUDED.
           05  PRSP-REASON              PIC X(20).
               88  PRSP-REASON-NSF          VALUE 'NSF'.
               88  PRSP-REASON-RESTRICTED   VALUE 'RESTRICTED'.
               88  PRSP-REASON-PARTIAL      VALUE 'PARTIAL-REVERSAL'.
               88  PRSP-REASON-ORIG-MISSING VALUE 'ORIG-NOT-FOUND'.
           05  FILLER                   PIC X(05).
