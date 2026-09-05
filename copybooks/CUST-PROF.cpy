      *****************************************************************
      * CUST-PROF - BEDROCK CUSTOMER PROFILE INQUIRY, CICS TRAN MTCP  *
      *                                                               *
      * REQUEST LENGTH 56, RESPONSE LENGTH 264.                       *
      * THE RESPONSE EMBEDS AN MTBCUST RECORD (200) AFTER THE COMMON  *
      * HEADER (64). THE EMBEDDED RECORD CARRIES CUST-TAX-ID-TOKEN,   *
      * A PII-VAULT TOKEN, NEVER THE CLEAR TAX ID. BEDROCK ITSELF     *
      * HOLDS THE CLEAR VALUE; THE ADAPTER IS NOT ENTITLED TO IT.     *
      *                                                               *
      * PROF-SCOPE CONTROLS HOW MUCH OF THE RECORD IS RETURNED.       *
      *   'NAME' - NAME AND SEGMENT ONLY, REST IS SPACES              *
      *   'FULL' - WHOLE RECORD, REQUIRES CHANNEL 'BUS ' OR 'BAT '    *
      *                                                               *
      * CHG 2021-06-22 A.BALARAMAN   INITIAL, PLAT-1877               *
      * CHG 2024-03-05 C.MBEKI       SCOPE FIELD, GIS-2290            *
      *****************************************************************
       01  CUST-PROF-REQUEST.
           05  PROF-HDR.
               10  PROF-TRAN-CODE       PIC X(04) VALUE 'MTCP'.
               10  PROF-CORRELATION-ID  PIC X(32).
               10  PROF-CHANNEL         PIC X(04).
           05  PROF-CUST-ID             PIC X(12).
           05  PROF-SCOPE               PIC X(04).
               88  PROF-SCOPE-NAME          VALUE 'NAME'.
               88  PROF-SCOPE-FULL          VALUE 'FULL'.
       01  CUST-PROF-RESPONSE.
           05  CRSP-HDR.
               10  CRSP-TRAN-CODE       PIC X(04).
               10  CRSP-CORRELATION-ID  PIC X(32).
               10  CRSP-RETURN-CODE     PIC 9(02).
               10  CRSP-ABEND-CODE      PIC X(04).
               10  CRSP-TIMESTAMP       PIC 9(14).
               10  FILLER               PIC X(08).
           05  CRSP-CUST-RECORD         PIC X(200).
