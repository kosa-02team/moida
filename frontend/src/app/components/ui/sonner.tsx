import { Toaster as Sonner, ToasterProps } from "sonner";

const Toaster = ({ ...props }: ToasterProps) => {
  return (
    <Sonner
      theme="light"
      className="toaster group"
      position="top-center"
      toastOptions={{
        classNames: {
          toast: 'group border-stone-200 bg-white text-stone-900 shadow-lg',
          description: 'text-stone-500',
          success: 'border-green-200 bg-green-50 text-green-900',
          error: 'border-red-200 bg-red-50 text-red-900',
          warning: 'border-yellow-200 bg-yellow-50 text-yellow-900',
          info: 'border-blue-200 bg-blue-50 text-blue-900',
        },
      }}
      {...props}
    />
  );
};

export { Toaster };
