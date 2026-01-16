import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createStory, type StoryCreateRequest } from '../../../../api/post';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Image, X, MapPin, Calendar, Users, Send } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Textarea } from '../../ui/textarea';
import { Input } from '../../ui/input';
import { Label } from '../../ui/label';
import { Badge } from '../../ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';

export function CreateStoryView() {
  const navigate = useNavigate();
  const { groupId } = useParams();
  const [content, setContent] = useState('');
  const [images, setImages] = useState<string[]>([]);
  const [location, setLocation] = useState('');
  const [taggedMembers, setTaggedMembers] = useState<string[]>(['???', '???']);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [linkedEvent, setLinkedEvent] = useState<string | null>('4? ?? ??');

  // Mock member data
  const members = ['???', '???', '???', '???', '???'];

  const handleImageUpload = () => {
    // ???? ?? ??? ??
    const mockImages = [
      'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=400&h=400&fit=crop',
      'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=400&h=400&fit=crop',
    ];
    setImages([...images, mockImages[images.length % 2]]);
    toast.info('???? ???????');
  };

  const removeImage = (index: number) => {
    setImages(images.filter((_, i) => i !== index));
  };

  const toggleMember = (name: string) => {
    setTaggedMembers(prev =>
      prev.includes(name)
        ? prev.filter(n => n !== name)
        : [...prev, name]
    );
  };

  const handleSubmit = async () => {
    if (!content.trim() && images.length === 0) {
      toast.error('?? ?? ???? ??????');
      return;
    }

    if (!groupId) {
      toast.error('?? ??? ?? ? ????');
      return;
    }

    setIsSubmitting(true);
    try {
      const request: StoryCreateRequest = {
        content: content.trim(),
        imagesUrl: images,
        // TODO: scheduleId? taggedMembers ?? ??
      };
      await createStory(Number(groupId), request);
      toast.success('???? ???????');
      navigate(-1);
    } catch (error) {
      console.error('??? ?? ??:', error);
      toast.error('??? ??? ??????');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-white pb-24">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate(-1)}
              className="-ml-2"
            >
              <ArrowLeft className="w-6 h-6 text-stone-800" />
            </Button>
            <h1 className="ml-2 text-lg font-semibold text-stone-800">??? ??</h1>
          </div>
          <Button
            onClick={handleSubmit}
            disabled={isSubmitting || (!content.trim() && images.length === 0)}
            className="bg-orange-500 hover:bg-orange-600 text-white rounded-full px-4"
          >
            {isSubmitting ? '?? ?...' : (
              <>
                <Send className="w-4 h-4 mr-1" />
                ??
              </>
            )}
          </Button>
        </div>
      </header>

      <div className="p-5 space-y-6">
        {/* Author Info */}
        <div className="flex items-center gap-3">
          <Avatar className="w-12 h-12">
            <AvatarFallback>?</AvatarFallback>
          </Avatar>
          <div>
            <p className="font-medium text-stone-900">???</p>
            <p className="text-xs text-stone-500">?? ?? ??</p>
          </div>
        </div>

        {/* Linked Event */}
        {linkedEvent && (
          <div className="flex items-center gap-2 p-3 bg-orange-50 rounded-xl">
            <Calendar className="w-4 h-4 text-orange-600" />
            <span className="text-sm text-orange-700">{linkedEvent}</span>
            <button
              onClick={() => setLinkedEvent(null)}
              className="ml-auto"
            >
              <X className="w-4 h-4 text-orange-600" />
            </button>
          </div>
        )}

        {/* Content Input */}
        <Textarea
          placeholder="???? ??? ??? ??????..."
          className="min-h-32 border-none shadow-none text-base resize-none focus-visible:ring-0 p-0"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          maxLength={500}
        />
        <p className="text-xs text-stone-400 text-right">{content.length}/500</p>

        {/* Images */}
        {images.length > 0 && (
          <div className="grid grid-cols-3 gap-2">
            {images.map((img, index) => (
              <div key={index} className="relative aspect-square rounded-xl overflow-hidden bg-stone-100">
                <img src={img} alt="" className="w-full h-full object-cover" />
                <button
                  onClick={() => removeImage(index)}
                  className="absolute top-1 right-1 w-6 h-6 bg-black/50 rounded-full flex items-center justify-center"
                >
                  <X className="w-4 h-4 text-white" />
                </button>
              </div>
            ))}
            {images.length < 9 && (
              <button
                onClick={handleImageUpload}
                className="aspect-square rounded-xl border-2 border-dashed border-stone-200 flex flex-col items-center justify-center text-stone-400 hover:border-orange-300 hover:text-orange-500 transition-colors"
              >
                <Image className="w-6 h-6" />
                <span className="text-xs mt-1">??</span>
              </button>
            )}
          </div>
        )}

        {/* Add Image Button (when no images) */}
        {images.length === 0 && (
          <button
            onClick={handleImageUpload}
            className="w-full p-6 border-2 border-dashed border-stone-200 rounded-xl flex flex-col items-center justify-center text-stone-400 hover:border-orange-300 hover:text-orange-500 transition-colors"
          >
            <Image className="w-8 h-8" />
            <span className="mt-2">?? ????</span>
            <span className="text-xs mt-1">?? 9??? ??</span>
          </button>
        )}

        {/* Location */}
        <div className="space-y-2">
          <Label className="flex items-center gap-2 text-stone-700">
            <MapPin className="w-4 h-4" />
            ?? ??
          </Label>
          <Input
            placeholder="??? ?????"
            className="h-11 bg-stone-50 border-stone-200 rounded-xl"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
          />
        </div>

        {/* Tag Members */}
        <div className="space-y-3">
          <Label className="flex items-center gap-2 text-stone-700">
            <Users className="w-4 h-4" />
            ??? ?? ??
          </Label>
          <div className="flex flex-wrap gap-2">
            {members.map(name => {
              const isSelected = taggedMembers.includes(name);
              return (
                <button
                  key={name}
                  onClick={() => toggleMember(name)}
                  className={`px-3 py-1.5 rounded-full text-sm transition-colors ${
                    isSelected
                      ? 'bg-orange-500 text-white'
                      : 'bg-stone-100 text-stone-600 hover:bg-stone-200'
                  }`}
                >
                  @{name}
                </button>
              );
            })}
          </div>
        </div>

        {/* Selected Tags Preview */}
        {taggedMembers.length > 0 && (
          <div className="bg-stone-50 rounded-xl p-4">
            <p className="text-sm text-stone-600">
              {taggedMembers.map((name, i) => (
                <span key={name}>
                  <span className="text-orange-600">@{name}</span>
                  {i < taggedMembers.length - 1 && ', '}
                </span>
              ))}
              ?? ??
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
