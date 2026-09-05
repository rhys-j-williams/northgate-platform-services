"""Inverse normal CDF without scipy. scipy is not on the treasury jump host's approved list
(GIS-0977 wheel provenance), so this is Acklam's rational approximation, accurate to ~1e-9.
Sits at the top level because engine.py imports it that way and nobody has moved it."""
import math

_a = (-3.969683028665376e01, 2.209460984245205e02, -2.759285104469687e02, 1.383577518672690e02, -3.066479806614716e01, 2.506628277459239e00)
_b = (-5.447609879822406e01, 1.615858368580409e02, -1.556989798598866e02, 6.680131188771972e01, -1.328068155288572e01)
_c = (-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e00, -2.549732539343734e00, 4.374664141464968e00, 2.938163982698783e00)
_d = (7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e00, 3.754408661907416e00)


def norm_ppf(p: float) -> float:
    if not 0.0 < p < 1.0:
        raise ValueError("p must be in (0,1)")
    plow, phigh = 0.02425, 1 - 0.02425
    if p < plow:
        q = math.sqrt(-2 * math.log(p))
        return (((((_c[0] * q + _c[1]) * q + _c[2]) * q + _c[3]) * q + _c[4]) * q + _c[5]) / ((((_d[0] * q + _d[1]) * q + _d[2]) * q + _d[3]) * q + 1)
    if p <= phigh:
        q = p - 0.5
        r = q * q
        return (((((_a[0] * r + _a[1]) * r + _a[2]) * r + _a[3]) * r + _a[4]) * r + _a[5]) * q / (((((_b[0] * r + _b[1]) * r + _b[2]) * r + _b[3]) * r + _b[4]) * r + 1)
    q = math.sqrt(-2 * math.log(1 - p))
    return -(((((_c[0] * q + _c[1]) * q + _c[2]) * q + _c[3]) * q + _c[4]) * q + _c[5]) / ((((_d[0] * q + _d[1]) * q + _d[2]) * q + _d[3]) * q + 1)
