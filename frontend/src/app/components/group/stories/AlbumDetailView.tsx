import { useState, useEffect } from 'react';
import { getRecentAlbums, getRecentPosts, type AlbumCardResponse } from '../../../../api/post';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { ArrowLeft, MoreHorizontal, Heart, MessageCircle, Plus, Grid, List, Calendar, MapPin } from 'lucide-react';
import { Button } from '../../ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../ui/dropdown-menu';
import { Tabs, TabsList, TabsTrigger } from '../../ui/tabs';

interface Story {
  id: string;
  images: string[];
  content: string;
  author: {
    id: string;
    name: string;
    avatar?: string;
  };
  likes: number;
  comments: number;
  createdAt: string;
  location?: string;
}

export function AlbumDetailView() {
  const navigate = useNavigate();
  const { groupId, albumId } = useParams();
  const [loading, setLoading] = useState(true);
  const [album, setAlbum] = useState<AlbumCardResponse | null>(null);
  const [stories, setStories] = useState<Story[]>([]);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('list');

  // API에서 앨범 정보 및 게시글 목록 가져오기
  useEffect(() => {
    async function fetchAlbumData() {
      if (!groupId) return;
      try {
        setLoading(true);
        // 앨범 목록에서 현재 앨범 찾기
        const albums = await getRecentAlbums(Number(groupId), 10);
        const foundAlbum = albums.find(a => String(a.postId) === albumId || String(a.scheduleId) === albumId);
        if (foundAlbum) {
          setAlbum(foundAlbum);
          // 해당 일정의 게시글들 가져오기
          const posts = await getRecentPosts(Number(groupId));
          const convertedStories: Story[] = posts
            .filter(p => p.scheduleId && String(p.scheduleId) === String(foundAlbum.scheduleId))
            .map(p => ({
              id: String(p.postId),
              images: p.imagesUrl || [],
              content: p.content,
              author: {
                id: String(p.writerId),
                name: p.writerName,
                avatar: '',
              },
              likes: p.postLikes,
              comments: p.commentCount,
              createdAt: p.createdAt,
              location: undefined,
            }));
          setStories(convertedStories);
        }
      } catch (error) {
        console.error('앨범 데이터 불러오기 실패:', error);
      } finally {
        setLoading(false);
      }
    }
    fetchAlbumData();
  }, [groupId, albumId]);

  if (loading || !album) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50 pb-20">
      {/* Header with Cover */}
      <div className="relative h-48 bg-stone-300">
        <img
          src={album?.coverImageUrl || ''}
          alt={album?.scheduleName || ''}
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
        
        {/* Top Bar */}
        <div className="absolute top-0 left-0 right-0 p-4 flex items-center justify-between">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate(-1)}
            className="bg-black/20 hover:bg-black/40 text-white rounded-full"
          >
            <ArrowLeft className="w-6 h-6" />
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="bg-black/20 hover:bg-black/40 text-white rounded-full"
              >
                <MoreHorizontal className="w-6 h-6" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem>앨범 수정</DropdownMenuItem>
              <DropdownMenuItem className="text-red-600">앨범 삭제</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {/* Album Info */}
        <div className="absolute bottom-4 left-4 right-4 text-white">
          <h1 className="text-2xl font-bold mb-1">{album?.scheduleName || ''}</h1>
          <div className="flex items-center gap-3 text-sm text-white/80">
            <span>{album?.imageCount || 0}개</span>
          </div>
        </div>
      </div>

      {/* View Mode Toggle */}
      <div className="p-4 bg-white border-b border-stone-100 flex items-center justify-between">
        <span className="text-sm text-stone-500">게시글 {stories.length}개</span>
        <div className="flex gap-1 bg-stone-100 p-1 rounded-lg">
          <button
            onClick={() => setViewMode('list')}
            className={`p-2 rounded-md transition-colors ${
              viewMode === 'list' ? 'bg-white shadow-sm' : 'text-stone-500'
            }`}
          >
            <List className="w-4 h-4" />
          </button>
          <button
            onClick={() => setViewMode('grid')}
            className={`p-2 rounded-md transition-colors ${
              viewMode === 'grid' ? 'bg-white shadow-sm' : 'text-stone-500'
            }`}
          >
            <Grid className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Stories */}
      {viewMode === 'list' ? (
        <div className="divide-y divide-stone-100">
          {stories.map(story => (
            <Link to={`../stories/${story.id}`} key={story.id}>
              <div className="bg-white">
                {/* Author */}
                <div className="p-4 flex items-center gap-3">
                  <Avatar className="w-10 h-10">
                    <AvatarImage src={story.author.avatar} />
                    <AvatarFallback>{story.author.name[0]}</AvatarFallback>
                  </Avatar>
                  <div>
                    <p className="font-medium text-stone-900">{story.author.name}</p>
                    <p className="text-xs text-stone-500">{story.createdAt}</p>
                  </div>
                </div>

                {/* Images */}
                <div className={`${story.images.length > 1 ? 'grid grid-cols-2 gap-0.5' : ''}`}>
                  {story.images.slice(0, 4).map((img, i) => (
                    <div
                      key={i}
                      className={`aspect-square bg-stone-100 relative ${
                        story.images.length === 3 && i === 0 ? 'row-span-2' : ''
                      }`}
                    >
                      <img src={img} alt="" className="w-full h-full object-cover" />
                      {i === 3 && story.images.length > 4 && (
                        <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                          <span className="text-white text-xl font-bold">+{story.images.length - 4}</span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                {/* Content & Actions */}
                <div className="p-4 space-y-2">
                  <div className="flex items-center gap-4 text-stone-600">
                  </div>
                  <p className="text-stone-800 line-clamp-2">{story.content}</p>
                </div>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <div className="p-2 grid grid-cols-3 gap-0.5">
          {stories.flatMap(story => 
            story.images.map((img, i) => (
              <Link to={`../stories/${story.id}`} key={`${story.id}-${i}`}>
                <div className="aspect-square bg-stone-100">
                  <img src={img} alt="" className="w-full h-full object-cover" />
                </div>
              </Link>
            ))
          )}
        </div>
      )}

      {/* FAB */}
      <div className="fixed bottom-20 right-4 z-40">
        <Link to="../stories/create">
          <Button size="lg" className="rounded-full w-14 h-14 shadow-lg bg-orange-500 hover:bg-orange-600 text-white p-0">
            <Plus className="w-7 h-7" />
          </Button>
        </Link>
      </div>
    </div>
  );
}
