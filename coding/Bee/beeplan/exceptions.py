
# beeplan/exceptions.py
class BeePlanError(Exception):
    """Base exception for BeePlan."""
    pass

class DataValidationError(BeePlanError):
    """Raised when input data is invalid or incomplete."""
    pass

class SchedulingError(BeePlanError):
    """Raised when the solver cannot produce a valid schedule."""
