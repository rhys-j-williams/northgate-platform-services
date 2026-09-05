      *****************************************************************
      * MTBACCT - BEDROCK ACCOUNT MASTER EXTRACT RECORD               *
      * LENGTH 136. FIXED BLOCK, LRECL=136, BLKSIZE=27200.            *
      *                                                               *
      * PRODUCED BY BEDROCK JOB MTBD140A AT END OF DAY AND CONSUMED   *
      * BY BEDROCK-ADAPTER-SERVICE OVER IBM MQ QUEUE                  *
      * MTB.BEDROCK.ACCT.OUT.                                         *
      *                                                               *
      * AMOUNT FIELDS ARE SIGNED ZONED DECIMAL. THE SIGN IS CARRIED   *
      * IN THE LOW ORDER BYTE. SEE PLATFORM-SERVICES/COPYBOOKS/       *
      * README.MD FOR THE OVERPUNCH TABLE.                            *
      *                                                               *
      * CHG 2019-04-02 A.BALARAMAN  ADDED ACCT-STATUS FOR PLAT-2201   *
      * CHG 2022-08-17 E.CASTELLANOS WIDENED ACCT-TYPE TO 20 FOR THE  *
      *                              TREASURY OPERATING PRODUCT.      *
      *****************************************************************
       01  MTBACCT-RECORD.
           05  ACCT-ID                  PIC X(16).
           05  ACCT-CUSTOMER-ID         PIC X(12).
           05  ACCT-TYPE                PIC X(20).
               88  ACCT-TYPE-CHECKING       VALUE 'CHECKING'.
               88  ACCT-TYPE-SAVINGS        VALUE 'SAVINGS'.
               88  ACCT-TYPE-CREDIT-CARD    VALUE 'CREDIT-CARD'.
               88  ACCT-TYPE-MORTGAGE       VALUE 'MORTGAGE'.
               88  ACCT-TYPE-AUTO-LOAN      VALUE 'AUTO-LOAN'.
               88  ACCT-TYPE-CERTIFICATE    VALUE 'CERTIFICATE'.
               88  ACCT-TYPE-BUS-CHECKING   VALUE 'BUSINESS-CHECKING'.
               88  ACCT-TYPE-BUS-SAVINGS    VALUE 'BUSINESS-SAVINGS'.
               88  ACCT-TYPE-TREASURY-OPER  VALUE 'TREASURY-OPERATING'.
           05  ACCT-NUMBER              PIC X(10).
           05  ACCT-ROUTING-NUMBER      PIC X(09).
           05  ACCT-CURRENT-BAL         PIC S9(13)  DISPLAY SIGN
                                        TRAILING INCLUDED.
           05  ACCT-AVAILABLE-BAL       PIC S9(13)  DISPLAY SIGN
                                        TRAILING INCLUDED.
           05  ACCT-OPENED-DATE.
               10  ACCT-OPENED-CC       PIC 9(02).
               10  ACCT-OPENED-YY       PIC 9(02).
               10  ACCT-OPENED-MM       PIC 9(02).
               10  ACCT-OPENED-DD       PIC 9(02).
           05  ACCT-STATUS              PIC X(10).
               88  ACCT-STATUS-OPEN         VALUE 'OPEN'.
               88  ACCT-STATUS-DORMANT      VALUE 'DORMANT'.
               88  ACCT-STATUS-RESTRICTED   VALUE 'RESTRICTED'.
               88  ACCT-STATUS-CLOSED       VALUE 'CLOSED'.
           05  ACCT-OWNER-NAME          PIC X(25).
