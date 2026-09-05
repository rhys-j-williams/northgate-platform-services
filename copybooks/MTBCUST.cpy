      *****************************************************************
      * MTBCUST - BEDROCK CUSTOMER PARTY RECORD                       *
      * LENGTH 200. FIXED BLOCK, LRECL=200, BLKSIZE=27200.            *
      *                                                               *
      * PRODUCED BY BEDROCK JOB MTBD100C. THE DIGITAL ESTATE READS    *
      * THIS RECORD FOR NAME AND SEGMENT ONLY. ADDRESS AND TAX        *
      * IDENTIFIERS ARE TOKENISED BY PII-VAULT-SERVICE BEFORE THEY    *
      * REACH ANY CHANNEL APPLICATION. DO NOT LOG THIS RECORD.        *
      *****************************************************************
       01  MTBCUST-RECORD.
           05  CUST-ID                  PIC X(12).
           05  CUST-SEGMENT             PIC X(16).
               88  CUST-SEG-CONSUMER        VALUE 'CONSUMER'.
               88  CUST-SEG-SMALL-BUSINESS  VALUE 'SMALL-BUSINESS'.
               88  CUST-SEG-TREASURY        VALUE 'TREASURY'.
           05  CUST-FIRST-NAME          PIC X(24).
           05  CUST-LAST-NAME           PIC X(32).
           05  CUST-ORG-NAME            PIC X(40).
           05  CUST-TAX-ID-TOKEN        PIC X(24).
           05  CUST-MOBILE              PIC X(16).
           05  CUST-ENROLLED-DATE       PIC 9(08).
           05  CUST-ADDR-POSTAL         PIC X(10).
           05  CUST-ADDR-STATE          PIC X(02).
           05  CUST-FLAGS.
               10  CUST-DECEASED-FLAG   PIC X(01).
               10  CUST-VULNERABLE-FLAG PIC X(01).
               10  CUST-PAPERLESS-FLAG  PIC X(01).
               10  CUST-DO-NOT-CALL     PIC X(01).
           05  FILLER                   PIC X(12).
