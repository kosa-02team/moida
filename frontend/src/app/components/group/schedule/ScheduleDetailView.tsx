import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Calendar, Clock, MapPin, Users, MessageCircle, Share2, Edit3, Trash2, Check, X } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '../../ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '../../ui/avatar';
import { Badge } from '../../ui/badge';
import { Textarea } from '../../ui/textarea';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../../ui/alert-dialog';
import { getSchedule, getScheduleParticipants, type ScheduleResponse, type ScheduleParticipantResponse } from '../../../../api/schedule';

interface Comment {
  id: string;
  author: string;
  avatar: string;
  content: string;
  createdAt: string;
}

export function ScheduleDetailView() {
  const navigate = useNavigate();
  const { groupId, scheduleId } = useParams();
  const [schedule, setSchedule] = useState<ScheduleResponse | null>(null);
  const [participants, setParticipants] = useState<ScheduleParticipantResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [myResponse, setMyResponse] = useState<'attending' | 'not_attending' | null>(null);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [comment, setComment] = useState('');
  const [comments, setComments] = useState<Comment[]>([]);

  useEffect(() => {
    async function fetchData() {
      if (!groupId || !scheduleId) return;
      try {
        setLoading(true);
        const [scheduleData, participantsData] = await Promise.all([
          getSchedule(Number(groupId), Number(scheduleId)),
          getScheduleParticipants(Number(groupId), Number(scheduleId))
        ]);
        setSchedule(scheduleData);
        setParticipants(participantsData);
        
        // 내 참석 상태 확인
        // TODO: 현재 사용자 ID와 비교
      } catch (error) {
        console.error('일정 상세 불러오기 실패:', error);
        toast.error('일정 정보를 불러오는데 실패했습니다.');
        navigate(-1);
      } finally {
        setLoading(false);
      }
    }
    fetchData();
  }, [groupId, scheduleId, navigate]);

  if (loading || !schedule) {
    return (
      <div className="min-h-screen bg-stone-50 flex items-center justify-center">
        <div className="text-stone-500">로딩 중...</div>
      </div>
    );
  }

  const eventDate = new Date(schedule.eventDate);
  const endDate = new Date(schedule.endDate);
  
  const attendingCount = participants.filter(p => p.attendanceStatus === 'ATTENDING').length;
  const notAttendingCount = participants.filter(p => p.attendanceStatus === 'NOT_ATTENDING').length;
  const pendingCount = participants.filter(p => p.attendanceStatus === 'PENDING').length;

  const handleResponse = (response: 'attending' | 'not_attending') => {
    setMyResponse(response);
    toast.success(response === 'attending' ? '참석으로 응답했습니다' : '불참으로 응답했습니다');
  };

  const handleDelete = () => {
    toast.success('일정이 삭제되었습니다');
    navigate(-1);
  };

  const handleAddComment = () => {
    if (!comment.trim()) return;
    
    setComments([
      ...comments,
      {
        id: String(Date.now()),
        author: '나',
        avatar: '',
        content: comment,
        createdAt: '방금 전',
      },
    ]);
    setComment('');
    toast.success('댓글이 등록되었습니다');
  };

  const handleShare = () => {
    navigator.clipboard.writeText(window.location.href);
    toast.success('링크가 복사되었습니다');
  };

  return (
    <div className="min-h-screen bg-stone-50 pb-24">
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
            <h1 className="ml-2 text-lg font-semibold text-stone-800">일정 상세</h1>
          </div>
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="icon" onClick={handleShare}>
              <Share2 className="w-5 h-5 text-stone-600" />
            </Button>
            {/* TODO: 주최자 확인 후 수정/삭제 버튼 표시 */}
          </div>
        </div>
      </header>

      <div className="p-5 space-y-5">
        {/* Title & Badge */}
        <div>
          <Badge className="bg-orange-100 text-orange-700 mb-2">일정</Badge>
          <h2 className="text-2xl font-bold text-stone-900">{schedule.scheduleName}</h2>
        </div>

        {/* Date & Time & Location */}
        <div className="bg-white rounded-2xl p-4 space-y-4 border border-stone-100">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center">
              <Calendar className="w-5 h-5 text-orange-600" />
            </div>
            <div>
              <p className="font-medium text-stone-900">
                {eventDate.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' })}
              </p>
              <p className="text-sm text-stone-500">
                {eventDate.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} - {endDate.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
              </p>
            </div>
          </div>

          {schedule.location && (
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                <MapPin className="w-5 h-5 text-blue-600" />
              </div>
              <div>
                <p className="font-medium text-stone-900">{schedule.location}</p>
                <button className="text-xs text-orange-600 mt-1">지도 보기</button>
              </div>
            </div>
          )}
        </div>

        {/* Description */}
        {schedule.description && (
          <div className="bg-white rounded-2xl p-4 border border-stone-100">
            <h3 className="font-bold text-stone-900 mb-2">상세 내용</h3>
            <p className="text-sm text-stone-600 leading-relaxed">{schedule.description}</p>
          </div>
        )}

        {/* Attendance Response */}
        <div className="bg-white rounded-2xl p-4 border border-stone-100">
          <h3 className="font-bold text-stone-900 mb-3">참석 여부</h3>
          <div className="grid grid-cols-2 gap-3">
            <Button
              variant={myResponse === 'attending' ? 'default' : 'outline'}
              className={`h-12 rounded-xl ${
                myResponse === 'attending' 
                  ? 'bg-green-500 hover:bg-green-600 text-white' 
                  : 'border-stone-200'
              }`}
              onClick={() => handleResponse('attending')}
            >
              <Check className="w-5 h-5 mr-2" />
              참석
            </Button>
            <Button
              variant={myResponse === 'not_attending' ? 'default' : 'outline'}
              className={`h-12 rounded-xl ${
                myResponse === 'not_attending' 
                  ? 'bg-red-500 hover:bg-red-600 text-white' 
                  : 'border-stone-200'
              }`}
              onClick={() => handleResponse('not_attending')}
            >
              <X className="w-5 h-5 mr-2" />
              불참
            </Button>
          </div>
        </div>

        {/* Attendees */}
        <div className="bg-white rounded-2xl p-4 border border-stone-100">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-bold text-stone-900">참석자</h3>
            <div className="flex items-center gap-2 text-xs">
              <span className="text-green-600">참석 {attendingCount}</span>
              <span className="text-red-500">불참 {notAttendingCount}</span>
              <span className="text-stone-400">미정 {pendingCount}</span>
            </div>
          </div>
          
          <div className="space-y-3">
            {participants.length > 0 ? (
              participants.map(participant => (
                <div key={participant.participantId} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Avatar className="w-9 h-9">
                      <AvatarFallback>{participant.realName[0]}</AvatarFallback>
                    </Avatar>
                    <span className="text-sm font-medium text-stone-900">{participant.realName}</span>
                  </div>
                  <Badge
                    variant="secondary"
                    className={`text-xs ${
                      participant.attendanceStatus === 'ATTENDING' 
                        ? 'bg-green-100 text-green-700' 
                        : participant.attendanceStatus === 'NOT_ATTENDING'
                          ? 'bg-red-100 text-red-700'
                          : 'bg-stone-100 text-stone-500'
                    }`}
                  >
                    {participant.attendanceStatus === 'ATTENDING' ? '참석' : participant.attendanceStatus === 'NOT_ATTENDING' ? '불참' : '미정'}
                  </Badge>
                </div>
              ))
            ) : (
              <p className="text-sm text-stone-500 text-center py-4">아직 참가자가 없습니다</p>
            )}
          </div>
        </div>

        {/* Comments */}
        <div className="bg-white rounded-2xl p-4 border border-stone-100">
          <div className="flex items-center gap-2 mb-4">
            <MessageCircle className="w-5 h-5 text-stone-600" />
            <h3 className="font-bold text-stone-900">댓글</h3>
            <span className="text-sm text-stone-500">{comments.length}</span>
          </div>

          {comments.length > 0 ? (
            <div className="space-y-4 mb-4">
              {comments.map(c => (
                <div key={c.id} className="flex gap-3">
                  <Avatar className="w-8 h-8">
                    <AvatarImage src={c.avatar} />
                    <AvatarFallback>{c.author[0]}</AvatarFallback>
                  </Avatar>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-stone-900">{c.author}</span>
                      <span className="text-xs text-stone-400">{c.createdAt}</span>
                    </div>
                    <p className="text-sm text-stone-600 mt-0.5">{c.content}</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-stone-500 text-center py-4">아직 댓글이 없습니다</p>
          )}

          <div className="flex gap-2">
            <Textarea
              placeholder="댓글을 입력하세요"
              className="min-h-10 resize-none"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
            <Button
              onClick={handleAddComment}
              disabled={!comment.trim()}
              className="bg-orange-500 hover:bg-orange-600 px-4"
            >
              등록
            </Button>
          </div>
        </div>
      </div>

      {/* Delete Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>일정 삭제</AlertDialogTitle>
            <AlertDialogDescription>
              정말 이 일정을 삭제하시겠습니까?
              삭제된 일정은 복구할 수 없습니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-red-500 hover:bg-red-600"
            >
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}



