      *****************************************************************
      * MTBTRAN - BEDROCK POSTED TRANSACTION RECORD                   *
      * LENGTH 160. FIXED BLOCK, LRECL=160, BLKSIZE=27200.            *
      *                                                               *
      * WRITTEN BY BEDROCK JOB MTBD210P FOR EVERY POSTING CYCLE AND   *
      * REPLAYED ONTO REDPANDA TOPIC MERIDIAN.TRANSACTIONS.POSTED BY  *
      * BEDROCK-ADAPTER-SERVICE.                                      *
      *                                                               *
      * TRAN-SETTLED-DATE IS SPACES WHILE THE AUTHORISATION HAS NOT   *
      * YET SETTLED. CONSUMERS MUST TREAT SPACES AS NULL AND NOT AS   *
      * ZERO. SEE INCIDENT INC0044182.                                *
      *****************************************************************
       01  MTBTRAN-RECORD.
           05  TRAN-ID                  PIC X(16).
           05  TRAN-ACCT-ID             PIC X(16).
           05  TRAN-POSTED-DATE         PIC 9(08).
           05  TRAN-SETTLED-DATE        PIC X(08).
           05  TRAN-AMOUNT              PIC S9(13)  DISPLAY SIGN
                                        TRAILING INCLUDED.
           05  TRAN-RUNNING-BAL         PIC S9(13)  DISPLAY SIGN
                                        TRAILING INCLUDED.
           05  TRAN-MCC                 PIC X(04).
           05  TRAN-CHANNEL             PIC X(08).
               88  TRAN-CHANNEL-CARD        VALUE 'CARD'.
               88  TRAN-CHANNEL-ACH         VALUE 'ACH'.
               88  TRAN-CHANNEL-WIRE        VALUE 'WIRE'.
               88  TRAN-CHANNEL-INTERNAL    VALUE 'INTERNAL'.
               88  TRAN-CHANNEL-PAYLINK     VALUE 'PAYLINK'.
               88  TRAN-CHANNEL-CHECK       VALUE 'CHECK'.
               88  TRAN-CHANNEL-ATM         VALUE 'ATM'.
               88  TRAN-CHANNEL-FEE         VALUE 'FEE'.
           05  TRAN-STATUS              PIC X(10).
               88  TRAN-STATUS-PENDING      VALUE 'PENDING'.
               88  TRAN-STATUS-POSTED       VALUE 'POSTED'.
               88  TRAN-STATUS-DISPUTED     VALUE 'DISPUTED'.
               88  TRAN-STATUS-REVERSED     VALUE 'REVERSED'.
           05  TRAN-DESCRIPTION         PIC X(64).
