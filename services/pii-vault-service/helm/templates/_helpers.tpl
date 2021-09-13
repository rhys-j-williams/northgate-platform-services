{{- define "svc.name" -}}{{ .Chart.Name }}{{- end -}}
{{- define "svc.labels" -}}
app.kubernetes.io/name: {{ include "svc.name" . }}
app.kubernetes.io/version: {{ .Values.image.tag | quote }}
app.kubernetes.io/managed-by: helm
meridian.bank/team: {{ index .Chart.Maintainers 0 "name" | default "platform-engineering" }}
{{- end -}}
