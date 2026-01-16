import React, { useState } from 'react';
import { ImageIcon } from 'lucide-react';
import { cn } from '../ui/utils';

const ERROR_IMG_SRC =
  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODgiIGhlaWdodD0iODgiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgc3Ryb2tlPSIjMDAwIiBzdHJva2UtbGluZWpvaW49InJvdW5kIiBvcGFjaXR5PSIuMyIgZmlsbD0ibm9uZSIgc3Ryb2tlLXdpZHRoPSIzLjciPjxyZWN0IHg9IjE2IiB5PSIxNiIgd2lkdGg9IjU2IiBoZWlnaHQ9IjU2IiByeD0iNiIvPjxwYXRoIGQ9Im0xNiA1OCAxNi0xOCAzMiAzMiIvPjxjaXJjbGUgY3g9IjUzIiBjeT0iMzUiIHI9IjciLz48L3N2Zz4KCg==';

interface ImageWithFallbackProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  fallbackIcon?: boolean;
}

export function ImageWithFallback({
  src,
  alt,
  className,
  style,
  fallbackIcon = false,
  ...rest
}: ImageWithFallbackProps) {
  const [didError, setDidError] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const handleError = () => {
    setDidError(true);
    setIsLoading(false);
  };

  const handleLoad = () => {
    setIsLoading(false);
  };

  if (didError) {
    if (fallbackIcon) {
      return (
        <div
          className={cn(
            'inline-flex items-center justify-center bg-stone-100 text-stone-400',
            className
          )}
          style={style}
        >
          <ImageIcon className="w-8 h-8" />
        </div>
      );
    }
    return (
      <div
        className={cn('inline-block bg-stone-100 text-center align-middle', className)}
        style={style}
      >
        <div className="flex items-center justify-center w-full h-full">
          <img
            src={ERROR_IMG_SRC}
            alt={alt || '이미지를 불러올 수 없습니다'}
            {...rest}
            data-original-url={src}
            className="opacity-50"
          />
        </div>
      </div>
    );
  }

  return (
    <div className={cn('relative', className)} style={style}>
      {isLoading && (
        <div className="absolute inset-0 bg-stone-100 animate-pulse flex items-center justify-center">
          <div className="w-8 h-8 border-2 border-stone-300 border-t-orange-500 rounded-full animate-spin" />
        </div>
      )}
      <img
        src={src}
        alt={alt || '이미지'}
        className={cn('transition-opacity duration-300', isLoading && 'opacity-0')}
        onError={handleError}
        onLoad={handleLoad}
        loading="lazy"
        {...rest}
      />
    </div>
  );
}
