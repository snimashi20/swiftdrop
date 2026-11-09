import React, { forwardRef } from 'react';

interface FormFieldProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const FormField = forwardRef<HTMLInputElement, FormFieldProps>(
  ({ label, error, ...props }, ref) => {
    return (
      <div className="flex flex-col gap-1 w-full">
        {label && <label className="text-sm font-medium text-gray-700">{label}</label>}
        <input
          ref={ref}
          {...props}
          className={`px-3 py-2 border rounded-md focus:outline-none focus:ring-2 ${
            error ? 'border-red-500 focus:ring-red-200' : 'border-gray-300 focus:ring-blue-200'
          } ${props.className || ''}`}
        />
        {error && <span className="text-xs text-red-500">{error}</span>}
      </div>
    );
  }
);

FormField.displayName = 'FormField';