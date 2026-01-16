import { useState } from 'react';
import { Flag } from 'lucide-react';
import { toast } from 'sonner';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import { Button } from '../ui/button';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { RadioGroup, RadioGroupItem } from '../ui/radio-group';

export type ReportType = 'post' | 'comment' | 'user' | 'group';

interface ReportDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  type: ReportType;
  targetName?: string;
}

const REPORT_REASONS: Record<ReportType, { value: string; label: string }[]> = {
  post: [
    { value: 'spam', label: '스팸/광고' },
    { value: 'inappropriate', label: '부적절한 콘텐츠' },
    { value: 'harassment', label: '괴롭힘/혐오 발언' },
    { value: 'copyright', label: '저작권 침해' },
    { value: 'other', label: '기타' },
  ],
  comment: [
    { value: 'spam', label: '스팸/광고' },
    { value: 'inappropriate', label: '부적절한 콘텐츠' },
    { value: 'harassment', label: '괴롭힘/혐오 발언' },
    { value: 'other', label: '기타' },
  ],
  user: [
    { value: 'spam', label: '스팸/광고 계정' },
    { value: 'harassment', label: '괴롭힘/혐오 발언' },
    { value: 'impersonation', label: '사칭' },
    { value: 'scam', label: '사기/금전 요구' },
    { value: 'inappropriate', label: '부적절한 행동' },
    { value: 'other', label: '기타' },
  ],
  group: [
    { value: 'spam', label: '스팸/광고 모임' },
    { value: 'inappropriate', label: '부적절한 콘텐츠' },
    { value: 'scam', label: '사기/금전 갈취' },
    { value: 'illegal', label: '불법 활동' },
    { value: 'harassment', label: '회원 괴롭힘' },
    { value: 'other', label: '기타' },
  ],
};

const TYPE_LABELS: Record<ReportType, string> = {
  post: '게시글',
  comment: '댓글',
  user: '사용자',
  group: '모임',
};

export function ReportDialog({ open, onOpenChange, type, targetName }: ReportDialogProps) {
  const [reason, setReason] = useState('');
  const [detail, setDetail] = useState('');

  const reasons = REPORT_REASONS[type];
  const typeLabel = TYPE_LABELS[type];

  const handleReport = () => {
    if (!reason) {
      toast.error('신고 사유를 선택해주세요');
      return;
    }
    toast.success('신고가 접수되었습니다. 검토 후 조치하겠습니다.');
    onOpenChange(false);
    setReason('');
    setDetail('');
  };

  const handleClose = () => {
    onOpenChange(false);
    setReason('');
    setDetail('');
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Flag className="w-5 h-5 text-orange-500" />
            {typeLabel} 신고
          </DialogTitle>
          <DialogDescription>
            {targetName && <span className="font-medium">"{targetName}"</span>}
            {targetName ? '을(를) 신고합니다. ' : ''}
            신고 사유를 선택해주세요.
          </DialogDescription>
        </DialogHeader>
        <div className="py-4 space-y-4">
          <div className="space-y-3">
            <Label className="font-medium">신고 사유</Label>
            <RadioGroup value={reason} onValueChange={setReason} className="space-y-2">
              {reasons.map(r => (
                <div key={r.value} className="flex items-center space-x-3 p-3 rounded-lg border border-stone-200 hover:bg-stone-50 transition-colors">
                  <RadioGroupItem value={r.value} id={`reason-${r.value}`} />
                  <Label htmlFor={`reason-${r.value}`} className="cursor-pointer flex-1">
                    {r.label}
                  </Label>
                </div>
              ))}
            </RadioGroup>
          </div>
          <div className="space-y-2">
            <Label className="font-medium">상세 내용 (선택)</Label>
            <Textarea
              placeholder="추가로 알려주실 내용이 있다면 입력해주세요"
              value={detail}
              onChange={(e) => setDetail(e.target.value)}
              className="min-h-[100px] resize-none"
            />
          </div>
          <div className="bg-stone-50 rounded-lg p-3">
            <p className="text-xs text-stone-600">
              허위 신고는 제재를 받을 수 있습니다. 신고 내용은 관리자가 검토하며, 
              처리 결과는 알림으로 안내드립니다.
            </p>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={handleClose}>
            취소
          </Button>
          <Button onClick={handleReport} className="bg-orange-500 hover:bg-orange-600">
            신고하기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
