import { HttpException, HttpStatus } from '@nestjs/common';

/** Wire shape shared with the Java services (common-starter ApiError). retail-web's error interceptor maps on `code`. */
export interface ApiErrorBody {
  code: string;
  message: string;
  status: number;
  correlationId: string;
  timestamp: string;
  violations: Array<{ field: string; message: string }>;
}

export class ApiException extends HttpException {
  constructor(public readonly code: string, message: string, status: HttpStatus, public readonly violations: ApiErrorBody['violations'] = []) {
    super(message, status);
  }

  static notFound(code: string, message: string): ApiException {
    return new ApiException(code, message, HttpStatus.NOT_FOUND);
  }
  static forbidden(code: string, message: string): ApiException {
    return new ApiException(code, message, HttpStatus.FORBIDDEN);
  }
  static unauthorised(code: string, message: string): ApiException {
    return new ApiException(code, message, HttpStatus.UNAUTHORIZED);
  }
  static badRequest(code: string, message: string): ApiException {
    return new ApiException(code, message, HttpStatus.BAD_REQUEST);
  }
  static conflict(code: string, message: string): ApiException {
    return new ApiException(code, message, HttpStatus.CONFLICT);
  }
  static upstream(code: string, message: string): ApiException {
    return new ApiException(code, message, HttpStatus.BAD_GATEWAY);
  }
}
