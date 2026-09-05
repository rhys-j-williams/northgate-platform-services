      *****************************************************************
      * ACCT-INQ - BEDROCK ACCOUNT INQUIRY, CICS TRANSACTION MTAI     *
      *                                                               *
      * REQUEST LENGTH 64, RESPONSE LENGTH 200.                       *
      * SENT BY BEDROCK-ADAPTER-SERVICE ON BEDROCK.REQ, ANSWERED ON   *
      * BEDROCK.RESP WITH THE SAME MQ CORRELATION ID.                 *
      *                                                               *
      * THE RESPONSE EMBEDS AN MTBACCT RECORD (136) AFTER THE COMMON  *
      * RESPONSE HEADER (64). A NON ZERO RETURN CODE MEANS THE        *
      * EMBEDDED RECORD IS SPACES AND MUST NOT BE PARSED.             *
      *                                                               *
      * RETURN CODES                                                  *
      *   00  OK                                                      *
      *   04  ACCOUNT NOT FOUND                                       *
      *   08  ACCOUNT RESTRICTED, INQUIRY REFUSED                     *
      *   12  BEDROCK UNAVAILABLE (EOD BATCH RUNNING)                 *
      *   16  ABEND, SEE ABEND CODE                                   *
      *                                                               *
      * CHG 2020-11-03 P.VENKATESAN  INITIAL, PLAT-1490               *
      * CHG 2023-02-14 J.HOLLINS     ADDED INQ-AS-OF FOR STATEMENTS   *
      *****************************************************************
       01  ACCT-INQ-REQUEST.
           05  INQ-HDR.
               10  INQ-TRAN-CODE        PIC X(04) VALUE 'MTAI'.
               10  INQ-CORRELATION-ID   PIC X(32).
               10  INQ-CHANNEL          PIC X(04).
                   88  INQ-CHANNEL-RETAIL   VALUE 'RTL '.
                   88  INQ-CHANNEL-BUSINESS VALUE 'BUS '.
                   88  INQ-CHANNEL-BATCH    VALUE 'BAT '.
           05  INQ-ACCT-ID              PIC X(16).
           05  INQ-AS-OF                PIC 9(08).
       01  ACCT-INQ-RESPONSE.
           05  RSP-HDR.
               10  RSP-TRAN-CODE        PIC X(04).
               10  RSP-CORRELATION-ID   PIC X(32).
               10  RSP-RETURN-CODE      PIC 9(02).
               10  RSP-ABEND-CODE       PIC X(04).
                   88  RSP-ABEND-NONE       VALUE '    '.
                   88  RSP-ABEND-ASRA       VALUE 'ASRA'.
                   88  RSP-ABEND-AEY7       VALUE 'AEY7'.
               10  RSP-TIMESTAMP        PIC 9(14).
               10  FILLER               PIC X(08).
           05  RSP-ACCT-RECORD          PIC X(136).
